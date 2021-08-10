var express = require('express');
var router = express.Router();
var indy = require('indy-sdk');
var common = require('./common');
var path = require('path');

const schemaProperties = ['organization', 'vaccine', 'doses', 'date', 'target'];

router.post('/schema', async function(req, res, next) {
  try {
    let did = global.db.did.did;
    let poolHandle = await common.openPoolLedger();
    let [schemaId, schema] = await indy.issuerCreateSchema(did, 'Covid-Certificate', '0.1', schemaProperties);
    let schemaRequest = await indy.buildSchemaRequest(did, schema);
    await indy.signAndSubmitRequest(poolHandle, global.wallet, did, schemaRequest)
    await indy.closePoolLedger(poolHandle);

    global.db.schemaId = schemaId;
    global.db.state = common.WalletState.SCHEMA_READY;
    common.writeDB(global.db);

    res.json({state: global.db.state, schemaId: schemaId});
  } catch (e) {
    res.json({ error: { message: e.message }});
  }
});

router.get('/schema', function(req, res, next) {
  // global.db.schema will be set on definition creation
  res.json(global.db.schema);
});

module.exports = router;
