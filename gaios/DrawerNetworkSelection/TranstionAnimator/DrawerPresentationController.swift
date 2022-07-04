import UIKit

class DrawerPresentationController: UIPresentationController {

    private var backgroundView: UIView!

    override var frameOfPresentedViewInContainerView: CGRect {
        var frame: CGRect = .zero
        guard let containerView = containerView else { return .zero }
        frame.size = size(forChildContentContainer: presentedViewController,
                          withParentContainerSize: containerView.bounds.size)
        return frame
    }

    override init(presentedViewController: UIViewController, presenting presentingViewController: UIViewController?) {
        super.init(presentedViewController: presentedViewController, presenting: presentingViewController)
        configureBackground()
    }

    override func size(forChildContentContainer container: UIContentContainer, withParentContainerSize parentSize: CGSize) -> CGSize {
        return CGSize(width: parentSize.width * 0.80, height: parentSize.height)
    }

    override func presentationTransitionWillBegin() {
        guard let bg = backgroundView else { return }
        containerView?.insertSubview(bg, at: 0)

        NSLayoutConstraint.activate(NSLayoutConstraint.constraints(withVisualFormat: "V:|[bg]|",
                                                                   options: [],
                                                                   metrics: nil,
                                                                   views: ["bg": bg]))

        NSLayoutConstraint.activate(NSLayoutConstraint.constraints(withVisualFormat: "H:|[bg]|",
                                                                   options: [],
                                                                   metrics: nil,
                                                                   views: ["bg": bg]))

        guard let coordinator = presentedViewController.transitionCoordinator else {
          bg.alpha = 1.0
          return
        }

        coordinator.animate(alongsideTransition: { _ in
          self.backgroundView.alpha = 1.0
        })
    }

    override func containerViewWillLayoutSubviews() {
      presentedView?.frame = frameOfPresentedViewInContainerView
    }

    @objc func dismiss(recognizer: UITapGestureRecognizer) {
      presentingViewController.dismiss(animated: true)
    }

    func configureBackground() {
        backgroundView = UIView()
        backgroundView.backgroundColor = UIColor(white: 0.0, alpha: 0.6)
        backgroundView.translatesAutoresizingMaskIntoConstraints = false
        let backgroundTap = UITapGestureRecognizer(target: self, action: #selector(dismiss))
        backgroundView.addGestureRecognizer(backgroundTap)
        let downSwipe = UISwipeGestureRecognizer(target: self, action: #selector(dismiss))
        downSwipe.direction = .left
        backgroundView.addGestureRecognizer(downSwipe)
    }
}
