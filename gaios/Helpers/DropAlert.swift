import UIKit

class DropAlert: UIView {

    var delay: Double = 2.0
    var height: CGFloat = 80
    var duration = 0.3

    var messageFont: UIFont = UIFont.boldSystemFont(ofSize: 16) {
        didSet {
            messageLabel.font = messageFont
        }
    }

    private var messageFrame: CGRect!
    private var messageLabel = UILabel()
    private let screenWidth = UIScreen.main.bounds.size.width
    private let screenHeight = UIScreen.main.bounds.size.height
    private let statusBarHeight = UIApplication.shared.statusBarFrame.size.height

    init() {
        super.init(frame: CGRect.zero)
        frame = frame()
        backgroundColor = UIColor.red
        messageFrame = CGRect(x: 10, y: statusBarHeight, width: frame.size.width - 10, height: 20)
        messageLabel = UILabel(frame: messageFrame)
        messageLabel.textAlignment = .center
        messageLabel.numberOfLines = 0
        messageLabel.lineBreakMode = .byWordWrapping
        messageLabel.textColor = UIColor.white
        messageLabel.font = messageFont
        addSubview(messageLabel)
        let dismissTap = UITapGestureRecognizer(target: self, action: #selector(viewTapped))
        self.addGestureRecognizer(dismissTap)
    }

    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    func success(message: String, delay: Double = 2.0) {
        alert(with: message, textColor: .white, backgroundColor: .customMatrixGreen(), delay: delay)
    }

    func info(message: String, delay: Double = 2.0) {
        alert(with: message, textColor: .white, backgroundColor: .infoBlue(), delay: delay)
    }

    func warning(message: String, delay: Double = 2.0) {
        alert(with: message, textColor: .white, backgroundColor: .warningYellow(), delay: delay)
    }

    func error(message: String, delay: Double = 2.0) {
        alert(with: message, textColor: .white, backgroundColor: .errorRed(), delay: delay)
    }

    private func alert(with message: String, textColor: UIColor, backgroundColor: UIColor, delay: Double) {
        self.delay = delay
        show(message, textColor: textColor, backgroundColor: backgroundColor)
    }

    private func frame() -> CGRect {
        return CGRect(x: 0.0, y: -height, width: screenWidth, height: height)
    }

    @objc private func viewTapped() {
        hide(self)
    }

    private func show(_ message: String, textColor: UIColor, backgroundColor: UIColor) {
        addSubviewToWindow(self)
        configure(message: message, textColor: textColor, backgroundColor: backgroundColor)

        UIView.animate(withDuration: duration, animations: {
            self.frame.origin.y = 0
        })
        perform(#selector(hide), with: self, afterDelay: delay)
    }

    private func addSubviewToWindow(_ view: UIView) {
        if superview == nil {
            let reverseWindows = UIApplication.shared.windows.reversed()
            for window in reverseWindows {
                if window.windowLevel == UIWindow.Level.normal
                    && !window.isHidden
                    && window.frame != CGRect.zero {
                    window.addSubview(view)
                    return
                }
            }
        }
    }

    private func configure(message: String, textColor: UIColor, backgroundColor: UIColor) {
        messageLabel.text = message
        messageLabel.frame.origin.y = height / 2

        messageLabel.textColor = textColor
        self.backgroundColor = backgroundColor
    }

    @objc private func hide(_ alertView: UIView) {
        UIView.animate(withDuration: duration, animations: {
            alertView.frame.origin.y = -self.height
        })
        perform(#selector(remove), with: alertView, afterDelay: delay)
    }

    @objc private func remove(_ alertView: UIButton) {
        alertView.removeFromSuperview()
    }
}
