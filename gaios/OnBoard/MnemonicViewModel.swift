import Foundation
import PromiseKit

class MnemonicViewModel {

    func validateMnemonic(_ mnemonic: String) -> Promise<Void> {
        if let validated = try? gaios.validateMnemonic(mnemonic: mnemonic),
           validated {
            return Promise().asVoid()
        }
        return Promise(error: LoginError.invalidMnemonic())
    }

}
