# Vaccination certificates using Indy

This repository contains an example of issuing vaccination certificates using [Indy](https://indy.readthedocs.io/en/latest/).

This example consists of two agents. A web agent is for an issuer who issues vaccination certificates.
It shows step by step guides what to be done in Indy to issue credentials.
A mobile agent is an android app to receive vaccination certificates and to present proofs of vaccination.

## SDKs and protocols

The agents do not use Aries frameworks. They use Indy SDK to handle certificates and proofs.
They use QR codes to initiate communication and use HTTP as a communication channel.
(A production service should use HTTPS instead of HTTP.)

## Ledger usage

The agents use [BCovrin test network](http://dev.greenlight.bcovrin.vonx.io/) for Indy ledger.
It can be changed easily by replacing `pool_transactions_genesis` file in the `web` folder.

The mobile agent does not use the pool. It gets schema and credential definition from the web agents
when it constructs proofs.

## Running the agents

`run_web.sh` runs the web agent with Docker if you have Docker Desktop.
You can also run the web agent without Docker by `cd web; npm install; npm start`.
You need Nodejs and Indy SDK installed on your desktop.
Be aware that [installing Indy SDK on macOS](https://github.com/hyperledger/indy-sdk/#macos) is tricky.

You can use the web agent by visiting http://localhost:3000.

You need Android Studio and an android device to run the mobile agent. You cannot use android simulator to run the mobile agent bacause it needs qrcode scanner and it does not contain libraries for the simulator.
The android device should use WIFI on the same network as the web agent. Or the mobile agent will not be able to connect to the web agent.

# More about the agents

## Web agent

Web agent is a web app:
- written in javascript with [NodeJS](https://nodejs.org/)
- use [Express](https://expressjs.com/) web framework
- use [Vue.js](https://vuejs.org/) frontend framework
- use [Bootstrap](https://getbootstrap.com/) CSS framework
- use [Libindy wrapper for NodeJS](https://github.com/hyperledger/indy-sdk/blob/master/wrappers/nodejs/README.md)

Web agent works as follows:
- creates a wallet named `demoWallet` and a pool named `demoPool` on startup. demoWallet is created at `~/.indy_client/wallet/demoWallet` and demoPool is created at `~/.indy_client/pool/demoPool` using genesis file at `web/pool_transactions_genesis`.
- gets it's IP address from the env variable `SERVER` or from the `en0` interface
- tells it's IP to the mobile agent via qrcode
- creates a DID with seed given by you. You have to register this DID to the ledger. This DID is used to create a schema and a credential definition. Schema is defined in `web/routes/schema.js`.
- stores it's status, did, schema, and credential definition in a file `db.json`. You can reset the web agent by removing db.json file and demoWallet folder. You need to delete demoPool folder if you want to change the pool.

Web agent provides the following REST APIs for mobile agent:
- `GET /credential/credOffer/{credId}`: get the credential offer
- `POST /credential/credRequest/{credId}`: request the credential
- `POST /credential/proof/{proofId}`: present a proof
- `GET /schema`: get the schema
- `GET /definition`: get the credential definition

## Mobile agent

Mobile agent is an android app:
- written in kotlin
- use [ML Kit's barcode scanning API](https://firebase.google.com/docs/ml-kit/read-barcodes)
- use [Libindy wrapper for Java](https://github.com/hyperledger/indy-sdk/tree/master/wrappers/java)

Mobile agent works as follows:
- sets env variable `EXTERNAL_STORAGE` to `applicationContext.filesDir` telling where to create wallet files.
- creates a wallet named `demoWallet` and open it. It takes some time.
- creates a master secret in the wallet and save the secret's ID in the `SharedPreference`.
- most of the logics are implemented in `WalletMainActivity.kt`. It scans the qrcode, parse it, and decide whether to get a credential or to present a proof.
- does not connect to the pool. The prover usually fetchs schema and credential definition from the ledger to create a proof. Which schema to fetch depends on the proof request. In this example, we use only one schema and one credential definition, so the mobile agent simply gets them from the web agent.

Notes on the libraries and dependencies:
- Libindy wrapper for Java contains [JNA](https://github.com/java-native-access/jna) which does not support android. Exclude the JNA and import the proper version that support android.
```gradle
    implementation ('org.hyperledger:indy:1.14.2') {
        exclude group: 'net.java.dev.jna', module: 'jna'
    }
    implementation 'net.java.dev.jna:jna:5.8.0@aar'
```
- Libindy wrapper for Java does not contain native libraries. `libindy.so` and `libc++_shared.so` should be located in the `wallet-app/app/src/main/jniLibs/{abi}` folder. You can find `libc++_shared.so` file in the NDK files if you install [NDK](https://developer.android.com/ndk).

## QR code data format

The QR codes generated by the web agent are json data with two types:

**Credential Offer**
```json
{
  "type": "cred_offer",
  "title": "Vaccination certificate",
  "to": "Alice",
  "offer_id": "abcdefg1234",
  "server": "10.1.1.20:3000"
}
```

**Proof Request**
```json
{
  "type": "cred_verify",
  "title": "Request proof of vaccination",
  "server": "10.1.1.20:3000",
  "proof_request": <json object>,
  "proof_id": "abcdefg5678"
}
```
