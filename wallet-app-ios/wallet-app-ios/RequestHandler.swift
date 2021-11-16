//
//  RequestHandler.swift
//  wallet-app-ios
//

import SwiftUI
import Indy

enum QRCodeType: Identifiable {
    case credOffer, proofRequest
    var id: Int {
        hashValue
    }
}

extension Data {
    func string() -> String {
        return String(decoding: self, as: UTF8.self)
    }
}

class RequestHandler: ObservableObject {
    static let shared = RequestHandler()
    @Published var confirmMessage = ""
    @Published var qrcodeType: QRCodeType?
    @Published var alertMessage = ""
    @Published var showAlert = false
    var qrcode: [String: Any]?

    func processCredentialOffer(_ json: [String: Any]) {
        qrcode = json
        confirmMessage = "This is a \(json["title"]!) for \(json["to"]!). Do you want to take it?"
        triggerAlert(type: .credOffer)
    }
    
    func processVerify(_ json: [String: Any]) {
        qrcode = json
        confirmMessage = "This is a \(json["title"]!). Do you want to proceed?"
        triggerAlert(type: .proofRequest)
    }
    
    func getCredential(menu: Binding<MainMenu?>) {
        menu.wrappedValue = .loading

        Task {
            do {
                try await tryGetCredential()
                menu.wrappedValue = nil
                showSimpleAlert(message: "Received a certificate.")
            } catch {
                menu.wrappedValue = nil
                showSimpleAlert(message: "Failed to receive a certificate.")
                print(error)
            }
        }
    }

    func tryGetCredential() async throws {
        let server = qrcode!["server"]!
        let offerId = qrcode!["offer_id"]!
        
        let url = URL(string: "http://\(server)/credential/credOffer/\(offerId)")!
        let offerData = try Data(contentsOf: url)
        let offer = try JSONSerialization.jsonObject(with: offerData, options: []) as! [String: Any]
        let offerJson = try JSONSerialization.data(withJSONObject: offer["cred_offer"]!, options: []).string()
        let defJson = try JSONSerialization.data(withJSONObject: offer["cred_def"]!, options: []).string()

        let (oneTimeDid, _) = try await IndyDid.createAndStoreMyDid("{}", walletHandle: indyWallet!)
        let (credentialRequestJson, credentialRequestMetadataJson) = try await IndyAnoncreds.proverCreateCredentialReq(forCredentialOffer: offerJson, credentialDefJSON: defJson, proverDID: oneTimeDid!, masterSecretID: masterSecretId!, walletHandle: indyWallet!)
        let requestJson = "{\"cred_request\":\(credentialRequestJson!)}"

        let reqUrl = "http://\(server)/credential/credRequest/\(offerId)"
        var request = URLRequest(url: URL(string: reqUrl)!)
        request.httpMethod = "POST"
        request.httpBody = requestJson.data(using: .utf8)
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        let (response, _) = try await URLSession.shared.data(for: request)
        let responseJson = try JSONSerialization.jsonObject(with: response, options: []) as! [String: Any]
        let credJson = try JSONSerialization.data(withJSONObject: responseJson["credential"]!, options: []).string()
        let revRegDefJson = try JSONSerialization.data(withJSONObject: responseJson["revRegDef"]!, options: []).string()
        try await IndyAnoncreds.proverStoreCredential(credJson, credID: nil, credReqMetadataJSON: credentialRequestMetadataJson!, credDefJSON: defJson, revRegDefJSON: revRegDefJson, walletHandle: indyWallet!)
    }
    
    func sendProof(menu: Binding<MainMenu?>) {
        menu.wrappedValue = .loading

        Task {
            do {
                try await trySendProof()
                menu.wrappedValue = nil
                showSimpleAlert(message: "Verification is complete.")
            } catch {
                menu.wrappedValue = nil
                showSimpleAlert(message: "Verification failed.")
                print(error)
            }
        }
    }
    
    func trySendProof() async throws {
        let server = qrcode!["server"] as! String
        let proofId = qrcode!["proof_id"] as! String
        let proofReq = qrcode!["proof_request"]!
        let proofReqJson = try JSONSerialization.data(withJSONObject: proofReq, options: []).string()

        let schemaJson = try Data(contentsOf: URL(string: "http://\(server)/schema")!)
        let credDefJson = try Data(contentsOf: URL(string: "http://\(server)/definition")!)
        
        let searchHandle = try await IndyAnoncreds.proverSearchCredentials(forProofRequest: proofReqJson, extraQueryJSON: nil, walletHandle: indyWallet!)
        guard let credsJson = try await IndyAnoncreds.proverFetchCredentials(forProofReqItemReferent: "attr1_referent", searchHandle: searchHandle, count: 10) else {
            print("Cannot find credentials for proof")
            return
        }

        let credentials = try JSONSerialization.jsonObject(with: credsJson.data(using: .utf8)!, options: []) as! [[String: Any]]
        let credential = credentials[0]["cred_info"] as! [String: Any]

        let credentialIdForAttribute1 = credential["referent"] as! String
        let schemaId = credential["schema_id"] as! String
        let credDefId = credential["cred_def_id"] as! String
        let credRevId = credential["cred_rev_id"] as! String
        try await IndyAnoncreds.proverCloseCredentialsSearchForProofReq(withHandle: searchHandle)

        let revinfoJson = try String(contentsOf: URL(string: "http://\(server)/revStates/\(credRevId)")!)
        let revinfo = try JSONSerialization.jsonObject(with: revinfoJson.data(using: .utf8)!, options: []) as! [String: Any]
        let timestamp = revinfo["timestamp"]!
        let revStates = revinfo["revStates"]!
        let revStatesJson = try JSONSerialization.data(withJSONObject: revStates, options: []).string()
        
        let requestedCredentials = [
            "self_attested_attributes": [:],
            "requested_predicates": [:],
            "requested_attributes":
                ["attr1_referent":
                    ["cred_id": credentialIdForAttribute1,
                    "revealed": true,
                    "timestamp": timestamp]]]
        let requestedCredentialsJson = try JSONSerialization.data(withJSONObject: requestedCredentials, options: []).string()

        let schemaObj = [schemaId: try JSONSerialization.jsonObject(with: schemaJson, options: [])]
        let schema = try JSONSerialization.data(withJSONObject: schemaObj, options: []).string()
        let credDefObj = [credDefId: try JSONSerialization.jsonObject(with: credDefJson, options: [])]
        let credentialDefs = try JSONSerialization.data(withJSONObject: credDefObj, options: []).string()

        let proofJson = try await IndyAnoncreds.proverCreateProof(forRequest: proofReqJson, requestedCredentialsJSON: requestedCredentialsJson, masterSecretID: masterSecretId!, schemasJSON: schema, credentialDefsJSON: credentialDefs, revocStatesJSON: revStatesJson, walletHandle: indyWallet!)

        var request = URLRequest(url: URL(string: "http://\(server)/credential/proof/\(proofId)")!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = proofJson!.data(using: .utf8)

        let (response, _) = try await URLSession.shared.data(for: request)
        let responseJson = try JSONSerialization.jsonObject(with: response, options: []) as! [String: String]
        let result = responseJson["result"]!

        print("result=\(result)")
        if (result != "OK") {
            throw NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "Proof failed: server returned \(result)"])
        }
    }
    
    func reportError() {
        showSimpleAlert(message: "Unrecognized qrcode")
    }

    func triggerAlert(type: QRCodeType) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
            self?.qrcodeType = type
        }
    }

    func showSimpleAlert(message: String) {
        alertMessage = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
            self?.showAlert = true
        }
    }
}
