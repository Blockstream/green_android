import UIKit

enum ButtonStyle {
    case primary
    case primaryGray
    case primaryDisabled
    case outlined
    case outlinedGray
    case outlinedWhite
    case inline
    case destructiveOutlined
}

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

final class CheckButton: UIButton {

    private let tapGesture = UITapGestureRecognizer()
    /// :nodoc:
    override func awakeFromNib() {
        super.awakeFromNib()

        setupUI()
    }

    deinit {
        removeGestureRecognizer(tapGesture)
    }

    /// :nodoc:
    override func draw(_ rect: CGRect) {
        super.draw(rect)

        setupUI()
    }

    /// Performs the first setup of the button.
    private func setupUI() {

        setTitle(nil, for: [.normal, .disabled, .selected])
        setBackgroundImage(UIImage(), for: .normal)
        setBackgroundImage(UIImage(named: "check"), for: .selected)
        layer.borderWidth = 1.0
        layer.borderColor = UIColor.customGrayLight().cgColor
        layer.cornerRadius = 3.0

        tapGesture.addTarget(self, action: #selector(didTap))
        addGestureRecognizer(tapGesture)
    }

    @objc private func didTap() {
      isSelected.toggle()
      sendActions(for: .touchUpInside)
    }
}

extension UIButton {

    func setStyle(_ type: ButtonStyle) {
        layer.cornerRadius = 4.0
        switch type {
        case .primary:
            backgroundColor = UIColor.customMatrixGreen()
            setTitleColor(.white, for: .normal)
            isEnabled = true
        case .primaryGray:
            backgroundColor = UIColor.customModalMedium()
            setTitleColor(.white, for: .normal)
            isEnabled = true
        case .primaryDisabled:
            backgroundColor = UIColor.customBtnOff()
            setTitleColor(UIColor.customGrayLight(), for: .normal)
            isEnabled = false
        case .outlined:
            backgroundColor = UIColor.clear
            setTitleColor(UIColor.customMatrixGreen(), for: .normal)
            tintColor = UIColor.customMatrixGreen()
            layer.borderWidth = 1.0
            layer.borderColor = UIColor.customMatrixGreen().cgColor
            layer.cornerRadius = 5.0
        case .outlinedGray:
            backgroundColor = UIColor.clear
            setTitleColor(UIColor.white, for: .normal)
            layer.borderWidth = 1.0
            layer.borderColor = UIColor.customGrayLight().cgColor
            layer.cornerRadius = 5.0
        case .outlinedWhite:
            backgroundColor = UIColor.clear
            setTitleColor(UIColor.white, for: .normal)
            layer.borderWidth = 1.0
            layer.borderColor = UIColor.white.cgColor
            layer.cornerRadius = 5.0
        case .inline:
            backgroundColor = UIColor.clear
            setTitleColor(UIColor.customMatrixGreen(), for: .normal)
        case .destructiveOutlined:
            backgroundColor = UIColor.clear
            cornerRadius = 5.0
            setTitleColor(UIColor.customDestructiveRed(), for: .normal)
            borderWidth = 1.0
            borderColor = UIColor.customDestructiveRed()
        }
    }
}
