import UIKit

@IBDesignable
class DesignableButton: UIButton {}

extension UIButton {
    private static var gradientLayers = [UIButton: (CAGradientLayer, CAGradientLayer)]()

    override open var isHighlighted: Bool {
        didSet {
            self.alpha = isHighlighted ? 0.6 : 1
        }
    }

    var enabledGradientLayer: CAGradientLayer {
        get {
            return createGradients().0
        }
    }

    var disabledGradientLayer: CAGradientLayer {
        get {
            return createGradients().1
        }
    }

    private func createHorizontalGradientLayer(colours: [UIColor]) -> CAGradientLayer {
        let gradient: CAGradientLayer = CAGradientLayer()
        gradient.frame = self.bounds
        gradient.colors = colours.map { $0.cgColor }
        gradient.startPoint = CGPoint(x: 0.0, y: 0.5)
        gradient.endPoint = CGPoint(x: 1.0, y: 0.5)
        return gradient
    }

    private func createEnabledGradient() -> CAGradientLayer {
        return createHorizontalGradientLayer(colours: [UIColor.customMatrixGreenDark(), UIColor.customMatrixGreen()])
    }

    private func createDisabledGradient() -> CAGradientLayer {
        return createHorizontalGradientLayer(colours: [UIColor.clear, UIColor.clear])
    }

    private func createGradients() -> (CAGradientLayer, CAGradientLayer) {
        if UIButton.gradientLayers[self] == nil {
            UIButton.gradientLayers[self] = (createEnabledGradient(), createDisabledGradient())
        }
        return UIButton.gradientLayers[self]!
    }

    func setGradient(_ enable: Bool) {
        layer.sublayers?.filter { $0 is CAGradientLayer }.forEach { $0.removeFromSuperlayer() }
        layer.insertSublayer(enable ? createEnabledGradient() : createDisabledGradient(), at: 0)
        backgroundColor = UIColor.clear
        layer.borderColor = UIColor.customTitaniumLight().cgColor
        layer.borderWidth = enable ? 0 : 1
        setTitleColor(enable ? .white : .customTitaniumLight(), for: .normal)
        backgroundColor = .clear
        shadowColor = enable ? .black : .clear
        setNeedsDisplay()
        isUserInteractionEnabled = enable
    }

    func updateGradientLayerFrame() {
        layer.sublayers?.filter { $0 is CAGradientLayer }.forEach {
            $0.frame = self.bounds
        }
        setNeedsDisplay()
        setNeedsLayout()
    }

    func insets(for content: UIEdgeInsets, image: CGFloat) {

        self.contentEdgeInsets = UIEdgeInsets(
            top: content.top,
            left: content.left,
            bottom: content.bottom,
            right: content.right + image
        )

        self.titleEdgeInsets = UIEdgeInsets(
            top: 0,
            left: image,
            bottom: 0,
            right: -image
        )
    }
}
