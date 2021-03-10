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

    static let tag = 0x70726f6772657373
    var message: String! {
        didSet { self.lblHint.text = self.message }
    }

    init() {
        super.init(frame: .zero)
        tag = Loader.tag
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

    @objc func startLoader(message: String = "") {
        if let window = UIApplication.shared.keyWindow {
            if loader == nil {
                let loader = Loader()
                window.addSubview(loader)
            }
            loader?.message = message
            loader?.activateConstraints(in: window)
            loader?.start()
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
