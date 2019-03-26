import Foundation
import UIKit

class QRImageGenerator {

    static func imageForTextWhite(text: String, frame: CGRect) -> UIImage? {
        let data = text.data(using: String.Encoding.ascii, allowLossyConversion: false)

        let filter = CIFilter(name: "CIQRCodeGenerator")
        guard let colorFilter = CIFilter(name: "CIFalseColor") else { return nil }
        filter!.setValue(data, forKey: "inputMessage")
        filter!.setValue("Q", forKey: "inputCorrectionLevel")
        colorFilter.setValue(filter!.outputImage, forKey: "inputImage")
        colorFilter.setValue(CIColor(color: UIColor.white), forKey: "inputColor1") // Background white
        colorFilter.setValue(CIColor(color: UIColor.black), forKey: "inputColor0")

        guard let qrCodeImage = colorFilter.outputImage else {
            return nil
        }
        let scaleX = frame.size.width / qrCodeImage.extent.size.width
        let scaleY = frame.size.height / qrCodeImage.extent.size.height
        let scaledImage = qrCodeImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))

        return UIImage(ciImage: scaledImage)
    }
}
