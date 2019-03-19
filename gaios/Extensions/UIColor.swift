import Foundation
import UIKit

extension UIColor {
    class func customMatrixGreen() -> UIColor {
        return UIColor(red: 0.0/255.0, green: 180.0/255.0, blue: 90.0/255.0, alpha: 1)
    }

    class func customMatrixGreenDark() -> UIColor {
        return UIColor(red: 27.0/255.0, green: 119.0/255.0, blue: 69.0/255.0, alpha: 1)
    }

    class func customTitaniumDark() -> UIColor {
        return UIColor(named: "customTitaniumDark")!
    }

    class func customLowerBar() -> UIColor {
        return UIColor(named: "customLowerBar")!
    }

    class func customTitaniumMedium() -> UIColor {
        return UIColor(named: "customTitaniumMedium")!
    }

    class func customTitaniumLight() -> UIColor {
        return UIColor(named: "customTitaniumLight")!
    }

    class func cardLight() -> UIColor {
        return UIColor.init(red: 0x0A / 255, green: 0x7C / 255, blue: 0x4A / 255, alpha: 1)
    }

    class func cardMedium() -> UIColor {
        return UIColor.init(red: 0x21 / 255, green: 0x40 / 255, blue: 0x49 / 255, alpha: 1)
    }

    class func cardDark() -> UIColor {
        return UIColor.init(red: 0x14 / 255, green: 0x1E / 255, blue: 0x28 / 255, alpha: 1)
    }

    class func errorRed() -> UIColor {
        return UIColor(named: "errorRed")!
    }
}
