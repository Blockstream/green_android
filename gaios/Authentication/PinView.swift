import Foundation
import UIKit
import PromiseKit

@IBDesignable
class PinView: UIView {
    @IBOutlet weak var attempts: UILabel!
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var deleteButton: UIButton!
    @IBOutlet var keyButton: [UIButton]?
    @IBOutlet var pinLabel: [UILabel]?
    @IBOutlet weak var title: UILabel!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    func getBackgroundImage(_ color: CGColor) -> UIImage? {
        UIGraphicsBeginImageContext(CGSize(width: 1, height: 1))
        guard UIGraphicsGetCurrentContext() != nil else { return nil }
        UIGraphicsGetCurrentContext()!.setFillColor(color)
        UIGraphicsGetCurrentContext()!.fill(CGRect(x: 0, y: 0, width: 1, height: 1))
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image
    }

    func reload() {
        cancelButton.setTitle(NSLocalizedString("id_clear", comment: "").uppercased(), for: .normal)
        deleteButton.contentMode = .center
        deleteButton.imageView?.contentMode = .scaleAspectFill
        let background = getBackgroundImage(UIColor.customMatrixGreenDark().cgColor)
        keyButton?.enumerated().forEach { (_, button) in
            button.setBackgroundImage(background, for: UIControl.State.highlighted)
        }
    }
}
