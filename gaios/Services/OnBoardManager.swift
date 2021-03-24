import Foundation
import UIKit
import PromiseKit

class OnBoardManager {
    static let shared = OnBoardManager()

    var params: OnBoardParams?

    func login() -> Promise<[String: Any]> {
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap {
            appDelegate?.disconnect()
            return try appDelegate?.connect(self.params?.network ?? "mainnet")
        }.then(on: bgq) {
            try getSession().registerUser(mnemonic: self.params?.mnemonic ?? "").resolve()
        }.then(on: bgq) { _ in
            try getSession().login(mnemonic: self.params?.mnemonic ?? "", password: self.params?.mnemomicPassword ?? "").resolve()
        }
    }

    func account() -> Account {
        return Account(name: params?.walletName ?? "", network: params?.network ?? "mainnet")
    }
}
