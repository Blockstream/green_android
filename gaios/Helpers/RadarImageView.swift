import UIKit

class RadarImageView: UIImageView {
    private var spinning: Bool = false

    func spin(with options: UIView.AnimationOptions) {
        UIView.animate(withDuration: 0.1, animations: {
            self.transform = self.transform.rotated(by: .pi / 12)
        }, completion: { _ in
            if self.spinning {
                self.spin(with: .curveLinear)
            } else if options != .curveEaseOut {
                self.spin(with: .curveEaseOut)
            }
        })
    }

    func startSpinning() {
        if !spinning {
            spinning = true
            spin(with: .curveEaseIn)
        }
    }

    func stopSpinning() {
        spinning = false
    }
}
