import Foundation
import UIKit

class QRImageGenerator {

    static func imageForTextWhite(text: String, frame: CGRect) -> UIImage? {
        let data = text.data(using: String.Encoding.ascii, allowLossyConversion: false)

        let filter = CIFilter(name: "CIQRCodeGenerator")
        filter!.setValue(data, forKey: "inputMessage")
        filter!.setValue("Q", forKey: "inputCorrectionLevel")

        let colorFilter = CIFilter(name: "CIFalseColor")
        colorFilter!.setValue(filter!.outputImage, forKey: "inputImage")
        colorFilter!.setValue(CIColor(color: UIColor.white), forKey: "inputColor1")
        colorFilter!.setValue(CIColor(color: UIColor.black), forKey: "inputColor0")

        guard let qrCodeImage = colorFilter?.outputImage else { return nil }

        // calculate resize radios
        let size = qrCodeImage.extent.size
        let widthRatio = frame.size.width / size.width
        let heightRatio = frame.size.height / size.height

        // create image from qrcode
        guard let cgImage = CIContext().createCGImage(qrCodeImage, from: qrCodeImage.extent) else { return nil }
        let dimension = CGSize(width: size.width * widthRatio, height: size.height * heightRatio)

        // transform image without interpolation
        UIGraphicsBeginImageContextWithOptions(dimension, true, 0)
        guard let context = UIGraphicsGetCurrentContext() else { return nil }
        context.interpolationQuality = .none
        context.translateBy(x: 0, y: dimension.height)
        context.scaleBy(x: 1.0, y: -1.0)
        context.draw(cgImage, in: context.boundingBoxOfClipPath)
        let result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return result
    }
}
