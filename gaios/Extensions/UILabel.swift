import UIKit

enum LabelStyle {
    case title
    case titleCard
    case txt
    case txtCard
    case err
    case sectionTitle
}

extension UILabel {
    func setStyle(_ type: LabelStyle) {
        switch type {
        case .title:
            textColor = .white
            font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        case .titleCard:
            textColor = .white
            font = UIFont.systemFont(ofSize: 16.0, weight: .semibold)
        case .txt:
            textColor = .white
            font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        case .txtCard:
            textColor = UIColor.gW40()
            font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        case .err:
            textColor = UIColor.customDestructiveRed()
            font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
        case .sectionTitle:
            textColor = UIColor.gW40()
            font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        }
    }
}

@IBDesignable
class DesignableLabel: UILabel {}

class CopyableLabel: DesignableLabel {

    override var canBecomeFirstResponder: Bool {
        return true
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(labelWasLongPressed))
        self.addGestureRecognizer(longPress)
        self.isUserInteractionEnabled = true
    }

    @objc func labelWasLongPressed(_ gesture: UIGestureRecognizer) {
        if gesture.state == .recognized,
            let gestureView = gesture.view,
            let superview = gestureView.superview,
            gestureView.becomeFirstResponder() {
            let copyMC = UIMenuController.shared
            copyMC.setTargetRect(gestureView.frame, in: superview)
            copyMC.setMenuVisible(true, animated: true)
        }
    }

    override func copy(_ sender: Any?) {
        UIPasteboard.general.string = text
    }

    override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
        return (action == #selector(UIResponderStandardEditActions.copy(_:)))
    }
}
