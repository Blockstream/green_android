import UIKit
import Foundation

extension UIImage {
    convenience init?(base64 str: String?) {
        guard let str = str, let encodedData = Data(base64Encoded: str) else { return nil }
        self.init(data: encodedData)
    }
}

extension UIImage {

  public func maskWithColor(color: UIColor) -> UIImage {

    UIGraphicsBeginImageContextWithOptions(self.size, false, self.scale)
    let context = UIGraphicsGetCurrentContext()!
    let rect = CGRect(origin: CGPoint.zero, size: size)
    color.setFill()
    self.draw(in: rect)
    context.setBlendMode(.sourceIn)
    context.fill(rect)
    let resultImage = UIGraphicsGetImageFromCurrentImageContext()!
    UIGraphicsEndImageContext()
    return resultImage
  }
}

extension UIImage {
    class func imageWithColor(color: UIColor) -> UIImage {
        let rect: CGRect = CGRect(x: 0, y: 0, width: 1, height: 1)
        UIGraphicsBeginImageContextWithOptions(CGSize(width: 1.0, height: 1.0), false, 0)
        color.setFill()
        UIRectFill(rect)
        let image: UIImage? = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image!
    }
}
