import UIKit

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
