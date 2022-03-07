import UIKit

protocol SliderViewDelegate: AnyObject {
    func sliderThumbIsMoving(_ sliderView: SliderView)
    func sliderThumbDidStopMoving(_ position: Int)
}

class SliderView: UIView {

    weak var delegate: SliderViewDelegate?
    var slideLbl = UILabel()
    var slideThumb = UIView()

    func commonInit() {
        self.translatesAutoresizingMaskIntoConstraints = false
        let pan = UIPanGestureRecognizer(target: self, action: #selector(onPan(_:)))
        self.addGestureRecognizer(pan)
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        commonInit()
    }

    @objc private func onPan(_ sender: UIPanGestureRecognizer) {

        let translation: CGPoint = sender.translation(in: self)
        delegate?.sliderThumbIsMoving(self)

        let thumbBoxC: CGPoint = CGPoint(x: self.slideThumb.center.x + translation.x, y: self.slideThumb.center.y)

        self.slideLbl.alpha = self.tag == 0 ? 1 - self.slideThumb.frame.origin.x / 100.0 : 1 - (self.frame.size.width - (self.slideThumb.frame.origin.x + self.slideThumb.frame.size.width)) / 100.0

        if self.slideThumb.frame.origin.x >= 0 && self.slideThumb.frame.origin.x <= self.frame.size.width - self.slideThumb.frame.size.width {
            self.slideThumb.center = thumbBoxC
        }

        switch sender.state {
        case .ended:
            if thumbBoxC.x > self.frame.size.width / 2.0 {
                UIView.animate(withDuration: 0.15, animations: {
                    self.slideThumb.center = CGPoint(x: self.frame.size.width - self.slideThumb.frame.size.width/2, y: self.slideThumb.center.y)
                }, completion: { _ in
                    self.delegate?.sliderThumbDidStopMoving(1)
                    self.slideLbl.alpha = 1.0
                    self.slideLbl.text = NSLocalizedString("id_sending", comment: "")
                })
            } else {
                UIView.animate(withDuration: 0.15, animations: {
                    self.slideThumb.center = CGPoint(x: self.bounds.origin.x + self.slideThumb.frame.size.width/2, y: self.slideThumb.center.y)
                }, completion: { _ in
                    self.delegate?.sliderThumbDidStopMoving(0)
                    self.slideLbl.alpha = 1.0
                    self.slideLbl.text = NSLocalizedString("id_slide_to_send", comment: "")
                })
            }
        default:
            break
        }
        sender.setTranslation(.zero, in: self)
    }

    override func draw(_ rect: CGRect) {

        let fH: CGFloat = self.frame.size.height
        self.layer.cornerRadius = fH / 2

        self.subviews.forEach { e in e.removeFromSuperview() }
        let bgH = 0.9

        let bg = UIView(frame: CGRect(x: 0.0, y: (self.frame.size.height * (1 - bgH) / 2.0), width: self.frame.size.width, height: self.frame.size.height * bgH))
        bg.backgroundColor = .clear
        bg.layer.cornerRadius = (self.frame.size.height * bgH) / 2.0
        bg.layer.borderWidth = 2.0
        bg.layer.borderColor = (UIColor.white.withAlphaComponent(0.5)).cgColor
        self.addSubview(bg)

        self.slideLbl = UILabel(frame: CGRect(x: 0.0, y: 0.0, width: self.frame.size.width, height: self.frame.size.height))
        self.slideLbl.textAlignment = .center
        self.slideLbl.font = .systemFont(ofSize: 16.0, weight: .semibold)
        self.slideLbl.text = NSLocalizedString("id_slide_to_send", comment: "")
        self.slideLbl.textColor = .white
        self.addSubview(self.slideLbl)

        self.slideThumb = UIView(frame: CGRect(x: self.bounds.origin.x, y: self.bounds.origin.y, width: fH, height: fH))
        self.slideThumb.layer.cornerRadius = fH / 2

        let offset: CGFloat = 12
        let iconView = UIImageView(frame: CGRect(x: offset, y: offset, width: fH - (2 * offset), height: fH - (2 * offset)))
        iconView.image = UIImage(named: "arrow")?.maskWithColor(color: UIColor.white)

        self.slideThumb.backgroundColor = UIColor.customMatrixGreen()
        self.slideThumb.addSubview(iconView)
        self.addSubview(self.slideThumb)
    }

    func reset() {
        UIView.animate(withDuration: 0.3, animations: {
            self.slideThumb.center = CGPoint(x: self.bounds.origin.x + self.slideThumb.frame.size.width / 2.0, y: self.slideThumb.center.y)
        }, completion: { _ in
            self.delegate?.sliderThumbDidStopMoving(0)
            self.slideLbl.alpha = 1.0
            self.slideLbl.text = NSLocalizedString("id_slide_to_send", comment: "")
        })
    }
}
