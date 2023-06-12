import Foundation
import gdk

enum MnemonicSize: Int {
    case _12 = 12
    case _24 = 24
}

struct OnBoardParams: Codable {
    var testnet: Bool?
    var walletName: String?
    var mnemonic: String?
    var mnemomicPassword: String?
    var mnemonicSize = Constants.mnemonicSizeDefault
    var accountId: String?
    var xpubHashId: String?

    static var shared = OnBoardParams()

    func toAccount() -> Account {
        let network: NetworkSecurityCase = testnet ?? false ? .testnetSS : .bitcoinSS
        return Account(id: accountId ?? UUID().uuidString,
                       name: walletName ?? "",
                       network: network,
                       xpubHashId: xpubHashId ?? "")
    }
}
