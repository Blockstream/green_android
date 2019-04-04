import Foundation
import UIKit

class Toast {
    static let SHORT: DispatchTimeInterval = DispatchTimeInterval.milliseconds(2000)
    static let LONG: DispatchTimeInterval = DispatchTimeInterval.milliseconds(3500)
    static let padding = CGFloat(20)

    class Label: UILabel {
        override func drawText(in rect: CGRect) {
            super.drawText(in: UIEdgeInsetsInsetRect(rect, UIEdgeInsets(top: padding, left: padding, bottom: padding, right: padding)))
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

        NSLayoutConstraint(item: label, attribute: NSLayoutAttribute.height, relatedBy: NSLayoutRelation.equal, toItem: nil, attribute: NSLayoutAttribute.height, multiplier: 1, constant: estimateHeight).isActive = true
        NSLayoutConstraint(item: label, attribute: NSLayoutAttribute.width, relatedBy: NSLayoutRelation.equal, toItem: nil, attribute: NSLayoutAttribute.width, multiplier: 1, constant: estimateWidth).isActive = true
        NSLayoutConstraint(item: label, attribute: NSLayoutAttribute.centerX, relatedBy: NSLayoutRelation.equal, toItem: view, attribute: NSLayoutAttribute.centerX, multiplier: 1, constant: 0).isActive = true
        NSLayoutConstraint(item: label, attribute: NSLayoutAttribute.centerY, relatedBy: NSLayoutRelation.equal, toItem: view, attribute: NSLayoutAttribute.centerY, multiplier: 1, constant: 0).isActive = true

        // Set autohidden after timeout
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + timeout) {
            view.removeFromSuperview()
        }
    }
}

class SnackBar: UIStackView {

    private static let padding = CGFloat(16)
    let button = UIButton()
    let label = Label()
    var controller: UITabBarController?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    class Label: UILabel {
        override func drawText(in rect: CGRect) {
            super.drawText(in: UIEdgeInsetsInsetRect(rect, UIEdgeInsets(top: padding, left: padding, bottom: padding, right: padding)))
        }
    }

    override func setup() {
        // setup stackView container
        axis = .horizontal
        distribution = .fill
        alignment = .fill
        translatesAutoresizingMaskIntoConstraints = false
        tag = 100
        isUserInteractionEnabled = true
        backgroundColor = UIColor.customTitaniumDark()

        // setup label message
        label.translatesAutoresizingMaskIntoConstraints = false
        label.numberOfLines = 1
        label.textAlignment = .left
        label.textColor = UIColor.white
        label.font = UIFont.systemFont(ofSize: 16)
        label.adjustsFontSizeToFitWidth = true
        label.backgroundColor = UIColor.customTitaniumMedium()
        addArrangedSubview(label)

        // setup action button
        button.setTitleColor(UIColor.errorRed(), for: .normal)
        button.titleLabel?.font = UIFont.boldSystemFont(ofSize: 14)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.backgroundColor = UIColor.customTitaniumMedium()
        addArrangedSubview(button)
    }

    override func updateConstraints() {
        super.updateConstraints()
        guard self.superview != nil else { return }
        let estimateRect = label.attributedText?.boundingRect(with: frame.size, options: [.usesFontLeading, .usesLineFragmentOrigin], context: nil)
        let estimateHeight = estimateRect!.height + SnackBar.padding * 2
        label.heightAnchor.constraint(equalToConstant: estimateHeight).isActive = true
        widthAnchor.constraint(equalTo: superview!.widthAnchor).isActive = true
        button.widthAnchor.constraint(equalToConstant: 100).isActive = true
        let bottomAnchorToView = bottomAnchor.constraint(equalTo: controller!.view.bottomAnchor, constant: -controller!.tabBar.frame.height)
        bottomAnchorToView.isActive = true
    }

    func addFromController(_ controller: UITabBarController) {
        self.controller = controller
        controller.view.viewWithTag(tag)?.removeFromSuperview()
        controller.view.addSubview(self)
        updateConstraints()
    }
}
