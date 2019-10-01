import UIKit

@IBDesignable
class DesignableView: UIView {
}

extension UIView {

    @IBInspectable
    var cornerRadius: CGFloat {
        get {
            return layer.cornerRadius
        }
        set {
            layer.cornerRadius = newValue
        }
    }

    @IBInspectable
    var borderWidth: CGFloat {
        get {
            return layer.borderWidth
        }
        set {
            layer.borderWidth = newValue
        }
    }

    @IBInspectable
    var borderColor: UIColor? {
        get {
            if let color = layer.borderColor {
                return UIColor(cgColor: color)
            }
            return nil
        }
        set {
            if let color = newValue {
                layer.borderColor = color.cgColor
            } else {
                layer.borderColor = nil
            }
        }
    }

    @IBInspectable
    var shadowRadius: CGFloat {
        get {
            return layer.shadowRadius
        }
        set {
            layer.shadowRadius = newValue
        }
    }

    @IBInspectable
    var shadowOpacity: Float {
        get {
            return layer.shadowOpacity
        }
        set {
            layer.shadowOpacity = newValue
        }
    }

    @IBInspectable
    var shadowOffset: CGSize {
        get {
            return layer.shadowOffset
        }
        set {
            layer.shadowOffset = newValue
        }
    }

    @IBInspectable
    var shadowColor: UIColor? {
        get {
            if let color = layer.shadowColor {
                return UIColor(cgColor: color)
            }
            return nil
        }
        set {
            if let color = newValue {
                layer.shadowColor = color.cgColor
            } else {
                layer.shadowColor = nil
            }
        }
    }

    func makeGradient(colours: [UIColor], locations: [NSNumber]?) -> CAGradientLayer {
        let gradient: CAGradientLayer = CAGradientLayer()
        gradient.frame = self.bounds
        gradient.colors = colours.map { $0.cgColor }
        gradient.locations = locations
        gradient.startPoint = CGPoint(x: 1, y: 1)
        gradient.endPoint = CGPoint(x: 0, y: 0)
        return gradient
    }

    func makeGradientCard() -> CAGradientLayer {
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        let colours = isLiquid ?
            [UIColor.cardBlueDark(), UIColor.cardBlueMediumDark(), UIColor.cardBlueMedium(), UIColor.cardBlueLight()] :
            [UIColor.cardDark(), UIColor.cardMediumDark(), UIColor.cardMedium(), UIColor.cardLight()]
        let gradient = makeGradient(colours: colours, locations: [0.0, 0.38, 0.68, 1.0])
        gradient.startPoint = CGPoint(x: 0.0, y: 1.0)
        gradient.endPoint = CGPoint(x: 1.0, y: 1.0)
        gradient.frame = self.bounds
        gradient.borderWidth = 2
        gradient.cornerRadius = 8
        gradient.borderColor = UIColor.customTitaniumDark().cgColor
        gradient.masksToBounds = true
        return gradient
    }

    // from https://www.hackingwithswift.com
    func findViewController() -> UIViewController? {
        if let nextResponder = self.next as? UIViewController {
            return nextResponder
        } else if let nextResponder = self.next as? UIView {
            return nextResponder.findViewController()
        } else {
            return nil
        }
    }

    @objc func setup() {
        let nibName = String(describing: type(of: self))
        guard let view = loadViewFromNib(nibName) else { return }
        view.frame = bounds
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.translatesAutoresizingMaskIntoConstraints = false
        addSubview(view)
        self.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "H:|[v]|", options: NSLayoutConstraint.FormatOptions(rawValue: 0), metrics: nil, views: ["v": view]))
        self.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "V:|[v]|", options: NSLayoutConstraint.FormatOptions(rawValue: 0), metrics: nil, views: ["v": view]))
    }

    func loadViewFromNib(_ nibName: String) -> UIView? {
        let bundle = Bundle(for: type(of: self))
        let nib = UINib(nibName: nibName, bundle: bundle)
        return nib.instantiate(withOwner: self, options: nil).first as? UIView
    }

    @objc func roundCorners(_ corners: UIRectCorner, radius: CGFloat) {
        let path = UIBezierPath(roundedRect: bounds,
                                byRoundingCorners: corners,
                                cornerRadii: CGSize(width: radius, height: radius))
        let mask = CAShapeLayer()
        mask.path = path.cgPath
        self.layer.mask = mask
    }
}
