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

    class func customMnemonicDark() -> UIColor {
        return UIColor(named: "customMnemonicDark")!
    }

    class func customModalDark() -> UIColor {
        return UIColor(named: "customModalBlueDark")!
    }

    class func customModalMedium() -> UIColor {
        return UIColor(named: "customModalMedium")!
    }

    class func customDestructiveRed() -> UIColor {
        return UIColor(named: "customDestructiveRed")!
    }

    class func customGrayLight() -> UIColor {
        return UIColor(named: "customGrayLight")!
    }

    class func customTextFieldBg() -> UIColor {
        return UIColor(named: "customTextFieldBg")!
    }

    class func customBtnOff() -> UIColor {
        return UIColor(named: "customBtnOff")!
    }

    class func cardLight() -> UIColor {
        return UIColor.init(red: 0 / 255, green: 144 / 255, blue: 72 / 255, alpha: 1)
    }

    class func cardMedium() -> UIColor {
        return UIColor.init(red: 28 / 255, green: 85 / 255, blue: 79 / 255, alpha: 1)
    }

    class func cardMediumDark() -> UIColor {
        return UIColor.init(red: 40 / 255, green: 54 / 255, blue: 70 / 255, alpha: 1)
    }

    class func cardDark() -> UIColor {
        return UIColor.init(red: 22 / 255, green: 30 / 255, blue: 39 / 255, alpha: 1)
    }

    class func cardBlueLight() -> UIColor {
        return UIColor.init(red: 0 / 255, green: 144 / 255, blue: 144 / 255, alpha: 1)
    }

    class func cardBlueMedium() -> UIColor {
        return UIColor.init(red: 30 / 255, green: 87 / 255, blue: 93 / 255, alpha: 1)
    }

    class func cardBlueMediumDark() -> UIColor {
        return UIColor.init(red: 40 / 255, green: 54 / 255, blue: 70 / 255, alpha: 1)
    }

    class func cardBlueDark() -> UIColor {
        return UIColor.init(red: 25 / 255, green: 34 / 255, blue: 39 / 255, alpha: 1)
    }

    class func blueLight() -> UIColor {
        return UIColor.init(red: 0x00 / 255, green: 0x70 / 255, blue: 0x6A / 255, alpha: 1)
    }

    class func errorRed() -> UIColor {
        return UIColor(named: "errorRed")!
    }

    class func warningYellow() -> UIColor {
        return UIColor(named: "warningYellow")!
    }

    class func infoBlue() -> UIColor {
        return UIColor(named: "infoBlue")!
    }

    class func accountOrange() -> UIColor {
        return UIColor(named: "accountOrange")!
    }

    class func accountGray() -> UIColor {
        return UIColor(named: "accountGray")!
    }

    class func accountLightBlue() -> UIColor {
        return UIColor(named: "accountLightBlue")!
    }
}

extension UIColor {
    class func gAccountLightBlue() -> UIColor {
        return UIColor(named: "gAccountLightBlue")!
    }
    class func gAccountOrange() -> UIColor {
        return UIColor(named: "gAccountOrange")!
    }
    class func gBlackBg() -> UIColor {
        return UIColor(named: "gBlackBg")!
    }
    class func gGrayBtn() -> UIColor {
        return UIColor(named: "gGrayBtn")!
    }
    class func gGrayCard() -> UIColor {
        return UIColor(named: "gGrayCard")!
    }
    class func gGreenMatrix() -> UIColor {
        return UIColor(named: "gGreenMatrix")!
    }
    class func gAccountTestGray() -> UIColor {
        return UIColor(named: "gAccountTestGray")!
    }
    class func gAccountTestLightBlue() -> UIColor {
        return UIColor(named: "gAccountTestLightBlue")!
    }
    class func gGrayTxt() -> UIColor {
        return UIColor(named: "gGrayTxt")!
    }
    class func gW40() -> UIColor {
        return UIColor(named: "gW40")!
    }
    class func gW60() -> UIColor {
        return UIColor(named: "gW60")!
    }
    class func gGreenFluo() -> UIColor {
        return UIColor(named: "gGreenFluo")!
    }
    class func gRedFluo() -> UIColor {
        return UIColor(named: "gRedFluo")!
    }
    class func gLightning() -> UIColor {
        return UIColor(named: "gLightning")!
    }
}

extension UIColor {

    func lighter(by percentage: CGFloat = 30.0) -> UIColor? {
        return self.adjust(by: abs(percentage) )
    }

    func darker(by percentage: CGFloat = 30.0) -> UIColor? {
        return self.adjust(by: -1 * abs(percentage) )
    }

    func adjust(by percentage: CGFloat = 30.0) -> UIColor? {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        if self.getRed(&red, green: &green, blue: &blue, alpha: &alpha) {
            return UIColor(red: min(red + percentage/100, 1.0),
                           green: min(green + percentage/100, 1.0),
                           blue: min(blue + percentage/100, 1.0),
                           alpha: alpha)
        } else {
            return nil
        }
    }
}
