import Foundation
import PromiseKit
import gdk

class MnemonicViewModel {

    func validateMnemonic(_ mnemonic: String) -> Promise<Void> {
        if let validated = try? gdk.validateMnemonic(mnemonic: mnemonic),
           validated {
            return Promise().asVoid()
        }
        return Promise(error: LoginError.invalidMnemonic())
    }

}
