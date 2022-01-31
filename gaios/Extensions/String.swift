import Foundation
import UIKit

extension String {

    static var chars: [Character] = {
        return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".map({$0})
    }()

    static func random(length: Int) -> String {
        var partial: [Character] = []

        for _ in 0..<length {
            let rand = Int(arc4random_uniform(UInt32(chars.count)))
            partial.append(chars[rand])
        }

        return String(partial)
    }

    func heightWithConstrainedWidth(width: CGFloat, font: UIFont) -> CGFloat {
        let constraintRect = CGSize(width: width, height: .greatestFiniteMagnitude)
        let boundingBox = self.boundingRect(with: constraintRect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: [NSAttributedString.Key.font: font], context: nil)
        return boundingBox.height
    }

    func localeFormattedString(_ digits: Int) -> String {
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .decimal
        numberFormatter.roundingMode = .down
        numberFormatter.maximumFractionDigits = digits
        numberFormatter.minimumFractionDigits = digits
        numberFormatter.locale = Locale(identifier: "en_EN")
        if let number = numberFormatter.number(from: self) {
            numberFormatter.locale = Locale.current
            if let localFormattedString = numberFormatter.string(from: number) {
                return localFormattedString
            }
        }
        return self
    }
    func unlocaleFormattedString(_ digits: Int) -> String {
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .decimal
        numberFormatter.roundingMode = .down
        numberFormatter.maximumFractionDigits = digits
        numberFormatter.minimumFractionDigits = digits
        numberFormatter.locale = Locale.current
        if let number = numberFormatter.number(from: self) {
            numberFormatter.groupingSeparator = ""
            numberFormatter.locale = Locale(identifier: "en_EN")
            if let localFormattedString = numberFormatter.string(from: number) {
                return localFormattedString
            }
        }
        return self
    }
}

extension NSMutableAttributedString {

    func setColor(color: UIColor, forText stringValue: String) {
        let range: NSRange = self.mutableString.range(of: stringValue, options: .caseInsensitive)
        self.addAttribute(NSAttributedString.Key.foregroundColor, value: color, range: range)
    }

    func setFont(font: UIFont, stringValue: String) {
        let range: NSRange = self.mutableString.range(of: stringValue, options: .caseInsensitive)
        self.addAttributes([NSAttributedString.Key.font: font], range: range)
    }

    func setAttributes(_ attrs: [NSAttributedString.Key: Any], for substring: String) {
        let range: NSRange = self.mutableString.range(of: substring, options: .caseInsensitive)
        self.addAttributes(attrs, range: range)
    }
}

extension StringProtocol {
    var firstUppercased: String { return prefix(1).uppercased() + dropFirst() }
    var firstCapitalized: String { return prefix(1).capitalized + dropFirst() }
}
