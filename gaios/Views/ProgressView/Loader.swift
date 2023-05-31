import UIKit
import Foundation
import RiveRuntime

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
    @IBOutlet weak var animateView: UIView!
    
    static let tag = 0x70726f6772657373
    var message: NSMutableAttributedString? {
        didSet { self.lblHint.attributedText = self.message }
    }

    init() {
        super.init(frame: .zero)
        tag = Loader.tag
        translatesAutoresizingMaskIntoConstraints = false
        setup()
        rectangle.cornerRadius = 5.0
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

    func start(_ isRive: Bool) {
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

        if !isRive {
            loadingIndicator.isAnimating = true
        } else {
            let riveView = RiveModel.animationRocket.createRiveView()
            animateView.addSubview(riveView)
            riveView.frame = CGRect(x: 0.0, y: 0.0, width: animateView.frame.width, height: animateView.frame.height)
        }
    }

    func stop() {
        loadingIndicator.isAnimating = false
    }
}

extension UIViewController {

    @objc var loader: Loader? {
        get {
            if let window = UIApplication.shared.windows.filter({ $0.isKeyWindow }).first {
                return window.viewWithTag(Loader.tag) as? Loader
            }
            return nil
        }
    }

    func startLoader(message: String = "", isRive: Bool = false) {
        startLoader(message: NSMutableAttributedString(string: message), isRive: isRive)
    }

    @objc func startLoader(message: NSMutableAttributedString, isRive: Bool = false) {
        if let window = UIApplication.shared.windows.filter({ $0.isKeyWindow }).first {
            if loader == nil {
                let loader = Loader()
                window.addSubview(loader)
            }
            loader?.message = message
            loader?.activateConstraints(in: window)
            if !(loader?.loadingIndicator.isAnimating ?? false) {

                // to change in "isRive"
                loader?.start(true)
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
