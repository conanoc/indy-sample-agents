var express = require('express');
var router = express.Router();

var indy = require('indy-sdk');
var crypto = require('crypto');
var qrcode = require('qrcode');
var os = require('os');
var common = require('./common');

router.post('/credential/proofRequest', async function(req, res, next) {
  let definitionId = global.db.definitionId;
  let nonce = await indy.generateNonce();
  let proofRequest = {
    'nonce': nonce,
    'name': 'COVID-PROOF',
    'version': '0.1',
    'requested_attributes': {
      'attr1_referent': {
        'name': 'vaccine',
        'restrictions': [{'cred_def_id': definitionId}]
      }
    },
    'non_revoked': {"to": common.getCurrentTimeInSeconds()}
  };

  let proofId = crypto.randomBytes(8).toString('hex');
  global.proofReqs[proofId] = proofRequest;
  global.proofResults[proofId] = 'PENDING';
  
  let qrcodeContent = {
    "type": "cred_verify",
    "title": "Request proof of vaccination",
    "server": global.server,
    "proof_request": proofRequest,
    "proof_id": proofId
  };
  console.log(JSON.stringify(qrcodeContent, null, 2));
  let dataUrl = await qrcode.toDataURL(JSON.stringify(qrcodeContent, null, 2));

  res.json({qrcode: dataUrl, proofId: proofId});
});

router.post('/credential/proof/:proofId', async function(req, res, next) {
  let result = false;
  try {
    let proof = req.body;
    // console.log('===proof===');
    // console.log(JSON.stringify(proof, null, 2));

    let did = global.db.did.did;
    let timestamp = proof.identifiers[0].timestamp;
    let poolHandle = await common.openPoolLedger();
    let getRevocRegRequest = await indy.buildGetRevocRegRequest(did, global.db.revRegDefId, timestamp);
    let getRevocRegResponse = await indy.submitRequest(poolHandle, getRevocRegRequest);
    let [, revRegValue, ] = await indy.parseGetRevocRegResponse(getRevocRegResponse);
    await indy.closePoolLedger(poolHandle);

    let revRefDefs = {
      [global.db.revRegDefId]: global.db.revRegDef
    }
    let revRegs = {
      [global.db.revRegDefId]: {
          [timestamp]: revRegValue
      }
    }
    result = await indy.verifierVerifyProof(global.proofReqs[req.params.proofId], proof, {[global.db.schemaId]: global.db.schema}, {[global.db.definitionId]: global.db.definition}, revRefDefs, revRegs);
  } catch (e) {
    console.log(e);
  }

  global.proofResults[req.params.proofId] = result ? "OK" : "FAIL";
  res.json({result: result ? "OK" : "FAIL"});
});

router.get('/credential/proof/:proofId', function(req, res, next) {
  res.json({result: global.proofResults[req.params.proofId]});
});

module.exports = router;
