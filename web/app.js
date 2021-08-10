var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
var indy = require('indy-sdk');
var common = require('./routes/common');
var os = require('os');

var indexRouter = require('./routes/index');
var walletRouter = require('./routes/wallet');
var stateRouter = require('./routes/state');
var schemaRouter = require('./routes/schema');
var definitionRouter = require('./routes/definition');
var credentialRouter = require('./routes/credential');
var proofRouter = require('./routes/proof');

var app = express();

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

app.use((req, res, next) => {
  res.set('Cache-Control', 'no-store');
  next();
});

app.use('/', indexRouter);
app.use('/', walletRouter);
app.use('/', stateRouter);
app.use('/', schemaRouter);
app.use('/', definitionRouter);
app.use('/', credentialRouter);
app.use('/', proofRouter);

async function openWallet() {
  let walletConfig = {'id': 'demoWallet'}
  let walletCredentials = {'key': '1234'}
  try {
    await indy.createWallet(walletConfig, walletCredentials)
  } catch(e) {
    if(e.message !== "WalletAlreadyExistsError") {
      throw e;
    }
  }

  global.wallet = await indy.openWallet(walletConfig, walletCredentials);
}
openWallet();
global.db = common.openDB();
global.poolName = "demoPool";
global.offers = {};
global.proofReqs = {};
global.proofResults = {};
global.credentialContent = {};

global.server = process.env.SERVER || (os.networkInterfaces().en0[1].address + ":3000");

module.exports = app;
