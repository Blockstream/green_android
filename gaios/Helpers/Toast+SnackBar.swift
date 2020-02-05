import Foundation
import UIKit

class Toast {
    static let SHORT: DispatchTimeInterval = DispatchTimeInterval.milliseconds(2000)
    static let LONG: DispatchTimeInterval = DispatchTimeInterval.milliseconds(3500)
    static let padding = CGFloat(20)

    class Label: UILabel {
        override func drawText(in rect: CGRect) {
            super.drawText(in: rect.inset(by: UIEdgeInsets(top: padding, left: padding, bottom: padding, right: padding)))
        }
    }

    static func show(_ message: String) {
        Toast.show(message, timeout: Toast.SHORT)
    }

    static func show(_ message: String, timeout: DispatchTimeInterval) {
        let window = UIApplication.shared.keyWindow!
        let view = UIView(frame: window.bounds)

        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.text = message
        label.lineBreakMode = NSLineBreakMode.byWordWrapping
        label.numberOfLines = 0
        label.textAlignment = .center
        label.backgroundColor = UIColor.init(red: 0xca/0xff, green: 0xd1/0xff, blue: 0xd7/0xff, alpha: 1)
        label.textColor = UIColor.init(red: 0x4a/0xff, green: 0x4a/0xff, blue: 0x4a/0xff, alpha: 1)
        label.cornerRadius = 4
        label.borderWidth = 1
        label.clipsToBounds = true
        label.layer.masksToBounds = true

        // Add label to view
        view.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        view.addSubview(label)
        window.addSubview(view)

        // Set constraints
        let estimateRect = label.attributedText?.boundingRect(with: view.frame.size, options: [.usesFontLeading, .usesLineFragmentOrigin], context: nil)
        let estimateHeight = estimateRect!.height + padding * 2
        let maxWidth = CGFloat(240)
        let estimateWidth = min(maxWidth, view.frame.width - padding * 2 * 2)

        NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.height, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.height, multiplier: 1, constant: estimateHeight).isActive = true
        NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: estimateWidth).isActive = true
        NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.centerX, relatedBy: NSLayoutConstraint.Relation.equal, toItem: view, attribute: NSLayoutConstraint.Attribute.centerX, multiplier: 1, constant: 0).isActive = true
        NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.centerY, relatedBy: NSLayoutConstraint.Relation.equal, toItem: view, attribute: NSLayoutConstraint.Attribute.centerY, multiplier: 1, constant: 0).isActive = true

        // Set autohidden after timeout
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + timeout) {
            view.removeFromSuperview()
        }
    }
}
