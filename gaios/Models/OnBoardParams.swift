import Foundation

struct OnBoardParams: Codable {
    var network: String?
    var walletName: String?
    var mnemonic: String?
    var mnemomicPassword: String?
    var singleSig = false
}
