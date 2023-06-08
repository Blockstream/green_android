import Foundation
import UIKit

class AddingCellModel {

    var title = NSAttributedString()

    init() {

        let lbl = "id_increase_the_security_of_your".localized
        let strs = ["id_adding_a_2fa"]

        let attributedText = NSMutableAttributedString.init(string: lbl)
        for str1 in strs {
            let range = (lbl as NSString).range(of: str1)
            attributedText.addAttribute(NSAttributedString.Key.underlineStyle, value: NSUnderlineStyle.single.rawValue, range: range)
            title = attributedText
        }
    }
}
