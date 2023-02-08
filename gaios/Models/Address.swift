import Foundation
import PromiseKit

struct Address: Codable {

    enum CodingKeys: String, CodingKey {
        case address = "address"
        case pointer = "pointer"
        case branch = "branch"
        case userPath = "user_path"
        case subaccount = "subaccount"
        case scriptType = "script_type"
        case addressType = "address_type"
        case script = "script"
        case subtype = "subtype"
    }

    let address: String
    let pointer: Int?
    let branch: Int?
    let subtype: UInt32?
    let userPath: [UInt32]?
    let subaccount: Int?
    let scriptType: Int?
    let addressType: String?
    let script: String?

    static func validate(with wallet: WalletItem, hw: HWProtocol, addr: Address, network: String) -> Promise<String> {
        return Promise { seal in
            _ = hw.newReceiveAddress(network: wallet.gdkNetwork,
                                     wallet: wallet,
                                     path: addr.userPath ?? [],
                                     csvBlocks: addr.subtype ?? 0)
                .subscribe(onNext: { data in
                    seal.fulfill(data)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }
}
