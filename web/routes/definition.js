var express = require('express');
var router = express.Router();
var indy = require('indy-sdk');
var common = require('./common');
var path = require('path');

router.post('/definition', async function(req, res, next) {
  try {
    let did = global.db.did.did;
    let schemaId = global.db.schemaId;
    let poolHandle = await common.openPoolLedger();

    let getSchemaRequest = await indy.buildGetSchemaRequest(did, schemaId);
    let getSchemaResponse = await indy.submitRequest(poolHandle, getSchemaRequest);
    let [_, schema] =  await indy.parseGetSchemaResponse(getSchemaResponse);
    let [definitionId, definitionJson] = await indy.issuerCreateAndStoreCredentialDef(global.wallet, did, schema, 'Demo', 'CL', '{"support_revocation": false}');
    let credDefRequest = await indy.buildCredDefRequest(did, definitionJson);

    await indy.signAndSubmitRequest(poolHandle, global.wallet, did, credDefRequest);
    await indy.closePoolLedger(poolHandle);

    global.db.definitionId = definitionId;
    global.db.definition = definitionJson;
    global.db.schema = schema;
    global.db.state = common.WalletState.DEFINITION_READY;
    common.writeDB(global.db);

    res.json({state: global.db.state, definitionId: definitionId});
  } catch (e) {
    res.json({ error: { message: e.message }});
  }
});

router.get('/definition', function(req, res, next) {
  res.json(global.db.definition);
});

module.exports = router;
