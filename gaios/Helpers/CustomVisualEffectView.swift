import Foundation
import UIKit

final class CustomVisualEffectView: UIVisualEffectView {

    init(effect: UIVisualEffect, intensity: CGFloat) {
        theEffect = effect
        customIntensity = intensity
        super.init(effect: nil)
    }

    required init?(coder aDecoder: NSCoder) { nil }

    deinit { animator?.stopAnimation(true) }

    override func draw(_ rect: CGRect) {
        super.draw(rect)
        effect = nil
        animator?.stopAnimation(true)
        animator = UIViewPropertyAnimator(duration: 1, curve: .linear) { [unowned self] in
            effect = theEffect
        }
        animator?.fractionComplete = customIntensity
    }

    private let theEffect: UIVisualEffect
    var customIntensity: CGFloat
    private var animator: UIViewPropertyAnimator?
}
