const fs = require('fs');
const path = require('path');
const indy = require('indy-sdk');
const crypto = require('crypto');

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

function encodeAttribute(text) {
  return BigInt('0x' + crypto.createHash('sha1').update(text).digest('hex')).toString();
}

exports.WalletState = WalletState;
exports.openDB = openDB;
exports.writeDB = writeDB;
exports.openPoolLedger = openPoolLedger;
exports.encodeAttribute = encodeAttribute;
