import Foundation

enum MnemonicSize: Int {
    case _12 = 12
    case _24 = 24
}

struct OnBoardParams: Codable {
    var network: String?
    var walletName: String?
    var mnemonic: String?
    var mnemomicPassword: String?
    var mnemonicSize = Constants.mnemonicSizeDefault
    var singleSig = false
    var accountId: String? = nil
}
