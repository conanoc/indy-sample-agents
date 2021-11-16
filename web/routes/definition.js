var express = require('express');
var router = express.Router();
var indy = require('indy-sdk');
var common = require('./common');
var path = require('path');

router.post('/definition', async function(req, res, next) {
  let poolHandle = await common.openPoolLedger();
  try {
    let did = global.db.did.did;
    let schemaId = global.db.schemaId;

    let getSchemaRequest = await indy.buildGetSchemaRequest(did, schemaId);
    let getSchemaResponse = await indy.submitRequest(poolHandle, getSchemaRequest);
    let [_, schema] =  await indy.parseGetSchemaResponse(getSchemaResponse);
    let [definitionId, definitionJson] = await indy.issuerCreateAndStoreCredentialDef(global.wallet, did, schema, 'Demo', 'CL', '{"support_revocation": true}');
    let credDefRequest = await indy.buildCredDefRequest(did, definitionJson);
    common.checkIndyResponse(await indy.signAndSubmitRequest(poolHandle, global.wallet, did, credDefRequest));

    let [revRegDefId, revRegDef, revRegEntry] = await indy.issuerCreateAndStoreRevocReg(global.wallet, did,
      "CL_ACCUM", "Demo", definitionId, {max_cred_num: 100, issuance_type: "ISSUANCE_ON_DEMAND"}, await common.getTailsWriter());
    let revocRegRequest = await indy.buildRevocRegDefRequest(did, revRegDef);
    common.checkIndyResponse(await indy.signAndSubmitRequest(poolHandle, global.wallet, did, revocRegRequest));
    let revocRegEntryRequest = await indy.buildRevocRegEntryRequest(did, revRegDefId, "CL_ACCUM", revRegEntry)
    common.checkIndyResponse(await indy.signAndSubmitRequest(poolHandle, global.wallet, did, revocRegEntryRequest));

    global.db.definitionId = definitionId;
    global.db.definition = definitionJson;
    global.db.revRegDefId = revRegDefId;
    global.db.revRegDef = revRegDef;
    global.db.schema = schema;
    global.db.state = common.WalletState.DEFINITION_READY;
    common.writeDB(global.db);

    res.json({state: global.db.state, definitionId: definitionId});
  } catch (e) {
    console.error(e);
    res.json({ error: { message: e.message }});
  } finally {
    await indy.closePoolLedger(poolHandle);
  }
});

router.get('/definition', function(req, res, next) {
  res.json(global.db.definition);
});

module.exports = router;
