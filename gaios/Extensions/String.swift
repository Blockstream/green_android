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

    static func toBtc(satoshi: UInt64, showDenomination: Bool = true) -> String {
        guard let settings = getGAService().getSettings() else { return "" }
        let res = try? getSession().convertAmount(input: ["satoshi" : satoshi])
        guard let _ = res, let data = res! else { return "" }
        guard let value = data[settings.denomination.rawValue] as? String else { return "" }
        return String(format: showDenomination ? "%@ %@" : "%@", value, settings.denomination.toString())
    }

    static func toFiat(satoshi: UInt64, showCurrency: Bool = true) -> String {
        guard let settings = getGAService().getSettings() else { return "" }
        let res = try? getSession().convertAmount(input: ["satoshi" : satoshi])
        guard let _ = res, let data = res! else { return "" }
        guard let value = data["fiat"] as? String else { return "" }
        return String(format: showCurrency ? "%@ %@" : "%@", value, settings.getCurrency())
    }

    static func toSatoshi(fiat: String) -> UInt64 {
        let res = try? getSession().convertAmount(input: ["fiat" : fiat])
        guard let _ = res, let data = res! else { return 0 }
        return data["satoshi"] as! UInt64
    }

    static func toSatoshi(amount: String) -> UInt64 {
        guard let settings = getGAService().getSettings() else { return 0 }
        let res = try? getSession().convertAmount(input: [settings.denomination.rawValue : amount])
        guard let _ = res, let data = res! else { return 0 }
        return data["satoshi"] as! UInt64
    }

    func heightWithConstrainedWidth(width: CGFloat, font: UIFont) -> CGFloat {
        let constraintRect = CGSize(width: width, height: .greatestFiniteMagnitude)
        let boundingBox = self.boundingRect(with: constraintRect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: [NSAttributedStringKey.font: font], context: nil)
        return boundingBox.height
    }

}

extension NSMutableAttributedString {

    func setColor(color: UIColor, forText stringValue: String) {
        let range: NSRange = self.mutableString.range(of: stringValue, options: .caseInsensitive)
        self.addAttribute(NSAttributedStringKey.foregroundColor, value: color, range: range)
    }

    func setFont(font: UIFont, stringValue: String) {
        let range: NSRange = self.mutableString.range(of: stringValue, options: .caseInsensitive)
        self.addAttributes([NSAttributedStringKey.font: font], range: range)
    }
}
