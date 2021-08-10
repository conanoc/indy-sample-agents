var express = require('express');
var router = express.Router();
const indy = require('indy-sdk');
var common = require('./common');

router.get('/didList', async function(req, res, next) {
  try {
    let list = await indy.listMyDidsWithMeta(global.wallet);
    res.json({ list: list });    
  } catch (e) {
    res.json({ error: { message: e.message }});
  }
});

router.post('/did', async function(req, res, next) {
  try {
    let info = {};
    if (req.body.seed && req.body.seed.length == 32) {
      info = { seed: req.body.seed };
    }
    let [did, key] = await indy.createAndStoreMyDid(global.wallet, info);
    await indy.setDidMetadata(global.wallet, did, req.body.memo);

    global.db.state = common.WalletState.DID_READY;
    global.db.did = { did: did, verkey: key };
    common.writeDB(global.db);

    res.json({ newDID: { did: did, verkey: key, metadata: req.body.memo } });
  } catch (e) {
    res.json({ error: { message: e.message }});
  }
});

module.exports = router;
