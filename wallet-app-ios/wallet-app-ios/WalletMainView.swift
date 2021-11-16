//
//  WalletMainView.swift
//  wallet-app-ios
//

import SwiftUI

enum MainMenu: Identifiable {
    case qrcode, list, loading
    var id: Int {
        hashValue
    }
}

struct WalletMainView: View {
    @State var menu: MainMenu?
    @StateObject var requestHandler = RequestHandler.shared

    var body: some View {
        ZStack {
            NavigationView {
                List {
                    Button(action: {
                        menu = .qrcode
                    }) {
                        Text("Scan a QR code")
                    }
                    
                    Button(action: {
                        menu = .list
                    }) {
                        Text("Credentials")
                    }
                }
                .navigationTitle("Wallet App")
                .listStyle(.plain)
            }
            .sheet(item: $menu) { item in
                switch item {
                case .qrcode:
                    QRScanView(handler: QRCodeHandler(menu: $menu))
                case .list:
                    CredentialListView()
                case .loading:
                    Text("Processing ...")
                }
            }
            .alert(item: $requestHandler.qrcodeType) { item in
                switch item {
                case .credOffer:
                    return Alert(title: Text("Get credential"), message: Text(requestHandler.confirmMessage), primaryButton: .default(Text("OK"), action: {
                        requestHandler.getCredential(menu: $menu)
                    }), secondaryButton: .cancel())
                case .proofRequest:
                    return Alert(title: Text("Send proof"), message: Text(requestHandler.confirmMessage), primaryButton: .default(Text("OK"), action: {
                        requestHandler.sendProof(menu: $menu)
                    }), secondaryButton: .cancel())                    
                }
            }
            .alert(requestHandler.alertMessage, isPresented: $requestHandler.showAlert) {}
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        WalletMainView()
    }
}
