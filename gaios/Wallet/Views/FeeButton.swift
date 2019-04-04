import Foundation
import UIKit

@IBDesignable
class FeeButton: UIButton {
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var timeLabel: UILabel!
    @IBOutlet weak var feerateLabel: UILabel!
    @IBOutlet weak var actionImage: UIImageView!
    @IBOutlet weak var view: UIView!

    let nibName = "FeeButton"
    var contentView: UIView?

    fileprivate var _isSelect: Bool = false
    var isSelect: Bool {
        get { return _isSelect }
        set { _isSelect = newValue; reload()}
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func setup() {
        contentView = loadViewFromNib()
        contentView!.frame = bounds
        contentView!.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addSubview(contentView!)
    }

    func loadViewFromNib() -> UIView! {
        let bundle = Bundle(for: type(of: self))
        let nib = UINib(nibName: nibName, bundle: bundle)
        let view = nib.instantiate(withOwner: self, options: nil).first as? UIView
        return view!
    }

    func reload() {
        actionImage.isHidden = !isSelect
        view.borderWidth = isSelect ? 1 : 0
    }

    func setTitle(_ title: String) {
        nameLabel.text = title
    }
}
