import Foundation
import UIKit

protocol AssetsManagerProtocol {
    func info(for key: String) -> AssetInfo
    func image(for key: String) -> UIImage
    func hasImage(for key: String?) -> Bool
    func loadAsync(session: SessionManager)
}
