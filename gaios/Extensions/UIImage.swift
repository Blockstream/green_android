import UIKit
import Foundation

extension UIImage {
    convenience init?(base64 str: String?) {
        guard let str = str, let encodedData = Data(base64Encoded: str) else { return nil }
        self.init(data: encodedData)!
    }
}
