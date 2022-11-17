import Foundation

typealias AssetAmountList = [(String, Int64)]

extension AssetAmountList {

    var registry: AssetsManager? { WalletManager.current?.registry }

    init(_ amounts: [String: Int64]) {
        self = amounts.map { ($0.key, $0.value) }
    }

    func sorted() -> [(String, Int64)] {
        return self.sorted(by: { (lhs, rhs) in
            guard let registry = registry else { return lhs.0 < rhs.0 }
            return registry.info(for: lhs.0) < registry.info(for: rhs.0)
        })
    }
}
