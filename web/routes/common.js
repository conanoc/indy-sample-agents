const fs = require('fs');
const path = require('path');
const indy = require('indy-sdk');
const crypto = require('crypto');
const os = require('os');

const WalletState = {
  READY: "ready",
  DID_READY: "did",
  SCHEMA_READY: "schema",
  DEFINITION_READY: "definition",
}

const dbFile = path.resolve(__dirname, '../db.json');
function openDB() {
  if(!fs.existsSync(dbFile)) {
    const db = { state: WalletState.READY };
    fs.writeFileSync(dbFile, JSON.stringify(db));
    return db;
  }

  const db = fs.readFileSync(dbFile, 'utf8');
  return JSON.parse(db);
}

function writeDB(db) {
  fs.writeFileSync(dbFile, JSON.stringify(db, null, 2));
}

async function openPoolLedger() {
  let poolConfig = {
    "genesis_txn": path.resolve(__dirname, '../pool_transactions_genesis')
  };
  try {
    await indy.createPoolLedgerConfig(global.poolName, poolConfig);
  } catch(e) {
    if(e.message !== "PoolLedgerConfigAlreadyExistsError") {
      throw e;
    }
  }

  await indy.setProtocolVersion(2);
  return await indy.openPoolLedger(global.poolName);
}

const tailsConfig = {"base_dir": os.homedir() + "/.indy_client/tails", "uri_pattern": ""};
async function getTailsWriter() {
  return await indy.openBlobStorageWriter("default", tailsConfig);
}

async function getTailsReader() {
  return await indy.openBlobStorageReader("default", tailsConfig);
}

function encodeAttribute(text) {
  return BigInt('0x' + crypto.createHash('sha1').update(text).digest('hex')).toString();
}

function getCurrentTimeInSeconds() {
  return Math.floor(Date.now() / 1000)
}

function checkIndyResponse(response) {
  if (!response) {
      throw new Error("ERROR in 'ensurePreviousRequestApplied' : response is undefined !")
  }
  if (response.op === "REJECT") {
      throw new Error("ERROR in 'ensurePreviousRequestApplied' : response.op is "+response.op+" and must be REPLY. Reason : "+response.reason)
  }
  if (response.op !== "REPLY") {
      throw new Error("ERROR in 'ensurePreviousRequestApplied' : response.op is "+response.op+" and must be REPLY")
  }
  if (!response.result) {
      throw new Error("ERROR in 'ensurePreviousRequestApplied' : response.result is undefined ! response=" + JSON.stringify(response))
  }
}

exports.WalletState = WalletState;
exports.openDB = openDB;
exports.writeDB = writeDB;
exports.openPoolLedger = openPoolLedger;
exports.encodeAttribute = encodeAttribute;
exports.getTailsWriter = getTailsWriter;
exports.getTailsReader = getTailsReader;
exports.tailsConfig = tailsConfig;
exports.getCurrentTimeInSeconds = getCurrentTimeInSeconds;
exports.checkIndyResponse = checkIndyResponse;
