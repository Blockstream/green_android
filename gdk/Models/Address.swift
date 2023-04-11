import Foundation
import PromiseKit
import hw

public struct Address: Codable {

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

    public let address: String?
    public let pointer: Int?
    public let branch: Int?
    public let subtype: UInt32?
    public let userPath: [UInt32]?
    public let subaccount: UInt32?
    public let scriptType: Int?
    public let addressType: String?
    public let script: String?

    public init(address: String? = nil, pointer: Int? = nil, branch: Int? = nil, subtype: UInt32? = nil, userPath: [UInt32]? = nil, subaccount: UInt32? = nil, scriptType: Int? = nil, addressType: String? = nil, script: String? = nil) {
        self.address = address
        self.pointer = pointer
        self.branch = branch
        self.subtype = subtype
        self.userPath = userPath
        self.subaccount = subaccount
        self.scriptType = scriptType
        self.addressType = addressType
        self.script = script
    }

    public static func validate(with wallet: WalletItem, hw: HWProtocol, addr: Address, network: String) -> Promise<String> {
        return Promise { seal in
            let network = wallet.gdkNetwork
            _ = hw.newReceiveAddress(chain: network.chain,
                                     mainnet: network.mainnet,
                                     multisig: !network.electrum,
                                     chaincode: wallet.recoveryChainCode,
                                     recoveryPubKey: wallet.recoveryPubKey,
                                     walletPointer: wallet.pointer,
                                     walletType: wallet.type.rawValue,
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
