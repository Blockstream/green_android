import UIKit
class ArcView: UIView {
    var step: Int = 0
    var steps: Int = 0

    init(frame: CGRect, step: Int, steps: Int) {
        self.step = step
        self.steps = steps
        super.init(frame: frame)
        self.backgroundColor = .clear
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    override func draw(_ rect: CGRect) {

        guard let context = UIGraphicsGetCurrentContext() else {
            print("could not get graphics context")
            return
        }

        let c = CGPoint(x: rect.size.width / 2.0, y: rect.size.width / 2.0)
        let borderWidth = 4.0
        let radius = (rect.size.width - borderWidth) / 2.0
        let startAngle = -CGFloat.pi / 2.0
        let endAngle = -CGFloat.pi / 2.0 + (CGFloat(step)) / CGFloat(steps) * 2 * CGFloat.pi
        context.setLineWidth(borderWidth)
        context.setStrokeColor(UIColor.customGrayLight().cgColor)

        context.addArc(center: CGPoint(x: c.x, y: c.y), radius: radius, startAngle: 0, endAngle: 2.0*CGFloat.pi, clockwise: false)
        context.strokePath()

        context.setStrokeColor(UIColor.customMatrixGreen().cgColor)

        context.beginPath()
        context.addArc(center: CGPoint(x: c.x, y: c.y), radius: radius, startAngle: startAngle, endAngle: endAngle, clockwise: false)
        context.strokePath()
    }
}
