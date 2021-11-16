//
//  QRCodeHandler.swift
//  wallet-app-ios
//

import SwiftUI
import QRScanner

class QRCodeHandler: QRScannerViewDelegate {
    @Binding var selectedMenu: MainMenu?
    
    public init(menu: Binding<MainMenu?>) {
        _selectedMenu = menu
    }

    func qrScannerView(_ qrScannerView: QRScannerView, didFailure error: QRScannerError) {
        print(error)
    }

    func qrScannerView(_ qrScannerView: QRScannerView, didSuccess code: String) {
        print(code)
        selectedMenu = nil

        let requestHandler = RequestHandler.shared
        let jsonObj = try? JSONSerialization.jsonObject(with: code.data(using: .utf8)!, options: [])
        guard let json = jsonObj as? [String: Any], let type = json["type"] as? String else {
            requestHandler.reportError()
            return
        }
        
        switch type {
        case "cred_offer":
            requestHandler.processCredentialOffer(json)
        case "cred_verify":
            requestHandler.processVerify(json)
        default:
            requestHandler.reportError()
        }
    }
}
