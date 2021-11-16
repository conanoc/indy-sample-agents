//
//  WalletOpener.swift
//  wallet-app-ios
//

import SwiftUI
import Indy

final class WalletState: ObservableObject {
  @Published var walletOpened: Bool = false
}

var indyWallet : IndyHandle?
var masterSecretId : String?

class WalletOpener : ObservableObject {

    func openWallet(walletState: WalletState) async throws {
        let walletConfig = ["id": "demoWallet"].toString()
        var walletCredentials: String?
        let wallet = IndyWallet.sharedInstance()!

        let userDefaults = UserDefaults.standard
        let walletExists = userDefaults.value(forKey:"walletExists") as? Bool

        if (walletExists == nil) {
            do {
                let key = try await IndyWallet.generateKey(forConfig: nil)
                walletCredentials = ["key": key, "key_derivation_method": "RAW"].toString()
                try await wallet.createWallet(withConfig: walletConfig, credentials: walletCredentials)
                userDefaults.set(key, forKey: "walletKey")
                userDefaults.set(true, forKey: "walletExists")
            } catch {
                if let err = error as NSError? {
                    print("Cannot create wallet: \(err.userInfo["message"] ?? "Unknown error")")
                    return
                }
            }
        } else {
            let key = userDefaults.value(forKey:"walletKey") as! String
            walletCredentials = ["key": key, "key_derivation_method": "RAW"].toString()
        }
        
        do {
            indyWallet = try await wallet.open(withConfig: walletConfig, credentials: walletCredentials)
        } catch {
            if let err = error as NSError? {
                print("Cannot open wallet: \(err.userInfo["message"] ?? "Unknown error")")
            }
            return
        }

        print("Wallet opened!")
        DispatchQueue.main.async {
            withAnimation { walletState.walletOpened = true }
        }
        
        masterSecretId = userDefaults.value(forKey:"masterSecret") as? String
        if masterSecretId == nil {
            masterSecretId = try! await IndyAnoncreds.proverCreateMasterSecret(nil, walletHandle: indyWallet!)
            userDefaults.set(masterSecretId, forKey: "masterSecret")
            print("masterSecret created: \(String(describing: masterSecretId))")
        }
    }
}
