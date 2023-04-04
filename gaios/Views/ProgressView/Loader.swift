import UIKit
import Foundation

@IBDesignable
class Loader: UIView {

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var rectangle: UIView!

    static let tag = 0x70726f6772657373
    var message: NSMutableAttributedString? {
        didSet { self.lblHint.attributedText = self.message }
    }

    init() {
        super.init(frame: .zero)
        tag = Loader.tag
        translatesAutoresizingMaskIntoConstraints = false
        setup()
        rectangle.cornerRadius = 10.0
        rectangle.borderWidth = 1.0
        rectangle.borderColor = .white.withAlphaComponent(0.05)
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

    func start() {
        self.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerXAnchor),
            loadingIndicator.centerYAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerYAnchor),
            loadingIndicator.widthAnchor
                .constraint(equalToConstant: self.loaderPlaceholder.frame.width),
            loadingIndicator.heightAnchor
                .constraint(equalTo: self.loadingIndicator.widthAnchor)
        ])

        loadingIndicator.isAnimating = true
    }

    func stop() {
        loadingIndicator.isAnimating = false
    }
}

extension UIViewController {

    @objc var loader: Loader? {
        get {
            return UIApplication.shared.keyWindow?.viewWithTag(Loader.tag) as? Loader
        }
    }

    func startLoader(message: String = "") {
        startLoader(message: NSMutableAttributedString(string: message))
    }

    @objc func startLoader(message: NSMutableAttributedString) {
        if let window = UIApplication.shared.keyWindow {
            if loader == nil {
                let loader = Loader()
                window.addSubview(loader)
            }
            loader?.message = message
            loader?.activateConstraints(in: window)
            if !(loader?.loadingIndicator.isAnimating ?? false) {
                loader?.start()
            }
        }
    }

    @objc func stopLoader() {
        UIApplication.shared.windows.forEach { window in
            window.subviews.forEach { view in
                if let loader = view.viewWithTag(Loader.tag) as? Loader {
                    loader.stop()
                    loader.removeFromSuperview()
                }
            }
        }
    }
}
