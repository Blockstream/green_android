import Foundation
import UIKit
import PromiseKit

class OnBoardManager {
    static let shared = OnBoardManager()

    var params: OnBoardParams?

    func account() -> Account {
        return Account(id: params?.accountId, name: params?.walletName ?? "", network: params?.network ?? "mainnet", isSingleSig: params?.singleSig ?? false)
    }

    var networkName: String {
        get {
            let params = OnBoardManager.shared.params
            let isSingleSig = params?.singleSig ?? false
            let ntw = params?.network ?? "mainnet"

            return (isSingleSig ? Constants.electrumPrefix + ntw : ntw)
        }
    }

}
