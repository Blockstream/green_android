import Foundation
import UIKit
import PromiseKit

class OnBoardManager {
    static let shared = OnBoardManager()

    var params: OnBoardParams?

    func account() -> Account {
        return Account(id: params?.accountId, name: params?.walletName ?? "", network: params?.network ?? "mainnet")
    }
}
