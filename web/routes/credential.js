var express = require('express');
var router = express.Router();

var indy = require('indy-sdk');
var crypto = require('crypto');
var qrcode = require('qrcode');
var os = require('os');
var common = require('./common');

var axios = require('axios');

function attributeObject(value) {
  return { raw: value, encoded: common.encodeAttribute(value) };
}

router.post('/credential', async function(req, res, next) {
  let definitionId = global.db.definitionId;

  let credOffer = await indy.issuerCreateCredentialOffer(global.wallet, definitionId);
  let offerId = crypto.randomBytes(8).toString('hex');
  global.offers[offerId] = credOffer;
  global.credentialContent[offerId] = req.body;

  let qrcodeContent = {
    "type": "cred_offer",
    "title": "Vaccination certificate",
    "to": req.body.target,
    "server": global.server,
    "offer_id": offerId
  };
  console.log(JSON.stringify(qrcodeContent, null, 2));
  let dataUrl = await qrcode.toDataURL(JSON.stringify(qrcodeContent, null, 2));

  res.json({qrcode: dataUrl});
});

router.get('/credential/credOffer/:credId', async function(req, res, next) {
  if (global.offers[req.params.credId] == undefined) {
    return res.json({ error: { message: "Unknown credential id. QRcode maybe obsolete." }});
  }
  
  res.json(
    {
      cred_def: global.db.definition,
      cred_offer: global.offers[req.params.credId]
    });
});

router.post('/credential/credRequest/:credId', async function(req, res, next) {
  let credId = req.params.credId;
  let credValues = {
    organization: attributeObject(global.credentialContent[credId].organization),
    vaccine: attributeObject(global.credentialContent[credId].vaccine),
    doses: attributeObject(global.credentialContent[credId].doses),
    target: attributeObject(global.credentialContent[credId].target),
    date: attributeObject(new Date().toUTCString())
  };
  // console.log('===request body===');
  // console.log(JSON.stringify(req.body, null, 2));

  try {
    let [credential, credRevId, revRegDelta] = await indy.issuerCreateCredential(global.wallet,
      global.offers[req.params.credId], req.body.cred_request, credValues, global.db.revRegDefId, await common.getTailsReader());
    // console.log('===credential===');
    // console.log(JSON.stringify(credential, null, 2));

    let poolHandle = await common.openPoolLedger();
    let did = global.db.did.did;
    let revocRegEntryRequest = await indy.buildRevocRegEntryRequest(did, global.db.revRegDefId, "CL_ACCUM", revRegDelta);
    let response = await indy.signAndSubmitRequest(poolHandle, global.wallet, did, revocRegEntryRequest);
    await indy.closePoolLedger(poolHandle);
    common.checkIndyResponse(response);

    res.json({credential: credential, revRegDef: global.db.revRegDef});
  } catch(e) {
    console.log(e);
    res.json({ error: { message: e.message }});
  }
});

router.get('/revStates/:credRevId', async function(req, res, next) {
  try {
    let poolHandle = await common.openPoolLedger();
    let timestamp = common.getCurrentTimeInSeconds();
    let did = global.db.did.did;
    let getRevocRegDeltaRequest = await indy.buildGetRevocRegDeltaRequest(did, global.db.revRegDefId, 0, timestamp);
    let getRevocRegDeltaResponse = await indy.submitRequest(poolHandle, getRevocRegDeltaRequest);
    let [, revRegDelta, ] = await indy.parseGetRevocRegDeltaResponse(getRevocRegDeltaResponse);
    await indy.closePoolLedger(poolHandle);

    let revState = await indy.createRevocationState(await common.getTailsReader(), global.db.revRegDef, revRegDelta, timestamp, req.params.credRevId);

    res.json({ revStates: {
      [global.db.revRegDefId]: {
        [timestamp]: revState
      }
    }, timestamp: timestamp});
  } catch(e) {
    console.log(e);
    res.json({ error: { message: e.message }});
  }
});

router.post('/credential/revoke/:credRevId', async function(req, res, next) {
  try {
    let revRegDelta = await indy.issuerRevokeCredential(global.wallet, await common.getTailsReader(), global.db.revRegDefId, req.params.credRevId);

    let poolHandle = await common.openPoolLedger();
    let did = global.db.did.did;
    let revocRegEntryRequest = await indy.buildRevocRegEntryRequest(did, global.db.revRegDefId, "CL_ACCUM", revRegDelta);
    let response = await indy.signAndSubmitRequest(poolHandle, global.wallet, did, revocRegEntryRequest);
    await indy.closePoolLedger(poolHandle);
    common.checkIndyResponse(response);

    res.json({});
  } catch(e) {
    console.log(e);
    res.json({ error: { message: e.message }});
  }
});

// Test get and store the credential
router.get('/credential/test/:credId', async function(req, res, next) {
  let offer = {
    cred_def: global.db.definition,
    cred_offer: global.offers[req.params.credId],
    issuer_did: global.db.did.did
  };

  let secretId = await indy.proverCreateMasterSecret(global.wallet, null);
  let [requestJson, requestMetadataJson] = await indy.proverCreateCredentialReq(global.wallet, global.db.did.did, JSON.stringify(offer.cred_offer), JSON.stringify(global.db.definition), secretId);
  let response = await axios.post(`http://localhost:3000/credential/credRequest/${req.params.credId}`, {cred_request: requestJson});
  await indy.proverStoreCredential(global.wallet, null, requestMetadataJson,
    response.data.credential, global.db.definition, response.data.revRegDef);
  res.send('OK');
});

module.exports = router;
