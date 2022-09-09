import Foundation
import UIKit

class AssetsManagerTestnet: Codable, AssetsManagerProtocol {
    func info(for key: String) -> AssetInfo {
        let denomination = WalletManager.current?.currentSession?.settings?.denomination
        let precision = UInt8(denomination?.digits ?? 8)
        let ticker = denomination?.string ?? "TEST"
        return AssetInfo(assetId: "btc", name: "Testnet", precision: precision, ticker: ticker)
    }
    func image(for key: String) -> UIImage {
        return UIImage(named: "ntw_testnet") ?? UIImage()
    }
    func hasImage(for key: String?) -> Bool { true }
    func loadAsync(session: SessionManager) { }
}
