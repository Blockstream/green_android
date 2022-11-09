import Foundation

typealias AssetAmountList = [(String, Int64)]

extension AssetAmountList {

    var registry: AssetsManager? { WalletManager.current?.registry }

    init(_ amounts: [String: Int64]) {
        self = amounts.map { ($0.key, $0.value) }
    }
    
    func sorted() -> [(String, Int64)] {
        return self.sorted(by: { (rhs, lhs) in
            let lbtc = getGdkNetwork("liquid").getFeeAsset()
            if rhs.0 == "btc" { return true
            } else if lhs.0 == "btc" { return false
            } else if rhs.0 == lbtc { return true
            } else if lhs.0 == lbtc { return false
            } else if registry?.hasImage(for: rhs.0) == true { return true
            } else if registry?.hasImage(for: lhs.0) == true { return false
            } else if registry?.info(for: rhs.0).ticker != nil { return true
            } else if registry?.info(for: lhs.0).ticker != nil { return false
            } else { return true}
        })
    }
}
