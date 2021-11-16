//
//  CredentialDetailView.swift
//  wallet-app-ios
//

import SwiftUI

struct CredentialDetailView: View {
    var credential: Credential
    
    var dateString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE, dd MMM yyyy HH:mm:ss z"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        if let date = formatter.date(from: credential.attrs["date"]!) {
            let local = DateFormatter()
            local.locale = NSLocale.current
            local.dateStyle = .medium
            local.timeStyle = .short
            return local.string(from: date)
        } else {
            return credential.attrs["date"]!
        }
    }

    var body: some View {
        Text("""
            Issuer organization: \(credential.attrs["organization"]!)
            Vaccine name: \(credential.attrs["vaccine"]!)
            Total doses: \(credential.attrs["round"]!)
            Recipient name: \(credential.attrs["target"]!)
            Date of issueance: \(dateString)

            Revocation ID: \(credential.cred_rev_id!)
        """)
            .padding(.all)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

struct CredentialDetailView_Previews: PreviewProvider {
    static var previews: some View {
        CredentialDetailView(credential: Credential(referent: "test", attrs: [:], schema_id: "", cred_def_id: ""))
    }
}
