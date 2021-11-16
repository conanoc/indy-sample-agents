var express = require('express');
var router = express.Router();
var fs = require('fs');
var common = require('./common');

router.get('/walletState', async function(req, res, next) {
  res.json({walletState: global.db.state, db: global.db});
});

module.exports = router;
