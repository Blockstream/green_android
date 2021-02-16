import Foundation
import PromiseKit

class Address: Codable {

    let address: String
    let pointer: UInt32?
    var branch: UInt32?
    let subtype: UInt32?

    static func generate(with wallet: WalletItem) -> Promise<Address> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().then(on: bgq) {_ in
            try getSession().getReceiveAddress(details: ["subaccount": wallet.pointer]).resolve()
        }.compactMap(on: bgq) { res in
            let result = res["result"] as? [String: Any]
            let data = try? JSONSerialization.data(withJSONObject: result!, options: [])
            return try? JSONDecoder().decode(Address.self, from: data!)
        }
    }

    static func validate(with wallet: WalletItem, hw: HWResolverProtocol, addr: Address) -> Promise<String> {
        let csv = wallet.type == "2of2"
        let csvBlocks = csv ? addr.subtype ?? 0 : 0
        return Promise { seal in
            _ = hw.newReceiveAddress(network: getNetwork(), subaccount: wallet.pointer, branch: addr.branch!, pointer: addr.pointer!, recoveryChainCode: wallet.recoveryChainCode, recoveryPubKey: wallet.recoveryPubKey, csvBlocks: csvBlocks)
                .subscribe(onNext: { data in
                    seal.fulfill(data)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }
}
