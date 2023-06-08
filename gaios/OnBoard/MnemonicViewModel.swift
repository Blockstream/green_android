import Foundation

import greenaddress

class MnemonicViewModel {

    func validateMnemonic(_ mnemonic: String) async throws {
        if let validated = try? await greenaddress.validateMnemonic(mnemonic: mnemonic),
           validated {
            return
        }
        throw LoginError.invalidMnemonic()
    }

}
