import Foundation
import UIKit

class AddingCellModel {

    var title = NSAttributedString()

    init() {

        var lbl = "increase the security fo your funds by adding a 2FA"
        let strs = ["adding a 2FA"]

        let attributedText = NSMutableAttributedString.init(string: lbl)
        for str1 in strs {
            let range = (lbl as NSString).range(of: str1)
            attributedText.addAttribute(NSAttributedString.Key.underlineStyle, value: NSUnderlineStyle.single.rawValue, range: range)
            title = attributedText
        }
    }
}
