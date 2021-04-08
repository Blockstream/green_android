import UIKit

class DrawerAnimator: NSObject {
    let isPresenting: Bool

    init(isPresenting: Bool) {
        self.isPresenting = isPresenting
        super.init()
    }
}

extension DrawerAnimator: UIViewControllerAnimatedTransitioning {

    func animateTransition(using transitionContext: UIViewControllerContextTransitioning) {
        let contextKey: UITransitionContextViewControllerKey = isPresenting ? .to : .from
        guard let controller = transitionContext.viewController(forKey: contextKey) else { return }

        if isPresenting {
            transitionContext.containerView.addSubview(controller.view)
        }

        let targetFrame = transitionContext.finalFrame(for: controller)
        var sourceFrame = targetFrame
        sourceFrame.origin.x = -transitionContext.containerView.frame.size.width

        let beginFrame = isPresenting ? sourceFrame : targetFrame
        let endFrame = isPresenting ? targetFrame : sourceFrame
        controller.view.frame = beginFrame

        UIView.animate(withDuration: transitionDuration(using: transitionContext), animations: {
            controller.view.frame = endFrame
        }, completion: { (done) in
            if !self.isPresenting {
                controller.view.removeFromSuperview()
            }
            transitionContext.completeTransition(done)
        })
    }

    func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        return 0.3
    }
}
