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
