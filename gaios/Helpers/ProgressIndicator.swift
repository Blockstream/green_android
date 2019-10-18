import UIKit
import Foundation

@IBDesignable
class ProgressIndicator: UIView {

    @IBOutlet weak var activityIndicator: UIActivityIndicatorView!
    @IBOutlet weak var messageLabel: UILabel!
    static let tag = 0x70726f6772657373
    var message: String! {
        didSet { self.messageLabel.text = self.message }
    }

    init() {
        super.init(frame: .zero)
        tag = ProgressIndicator.tag
        translatesAutoresizingMaskIntoConstraints = false
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func activateConstraints(in window: UIWindow) {
        NSLayoutConstraint.activate([
            self.leadingAnchor.constraint(equalTo: window.leadingAnchor),
            self.trailingAnchor.constraint(equalTo: window.trailingAnchor),
            self.topAnchor.constraint(equalTo: window.topAnchor),
            self.bottomAnchor.constraint(equalTo: window.bottomAnchor)
        ])
    }
}

extension UIViewController {
    // Add custom progress indicator

    @objc var progressIndicator: ProgressIndicator? {
        get {
            return UIApplication.shared.keyWindow?.viewWithTag(ProgressIndicator.tag) as? ProgressIndicator
        }
    }

    @objc func startAnimating(message: String = "") {
        if let window = UIApplication.shared.keyWindow {
            if progressIndicator == nil {
                let progressIndicator = ProgressIndicator()
                window.addSubview(progressIndicator)
            }
            progressIndicator?.activateConstraints(in: window)
            progressIndicator?.activityIndicator.startAnimating()
            progressIndicator?.message = message
        }
    }

    @objc func stopAnimating() {
        progressIndicator?.activityIndicator.stopAnimating()
        progressIndicator?.removeFromSuperview()
    }
}
