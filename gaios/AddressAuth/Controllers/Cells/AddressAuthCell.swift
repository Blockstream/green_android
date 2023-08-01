import UIKit

class AddressAuthCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblAddress: UILabel!
    @IBOutlet weak var lblTx: UILabel!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnSign: UIButton!
    
    var model: AddressAuthCellModel!
    var onCopy: (() -> Void)?
    var onSign: (() -> Void)?
    
    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        [btnCopy, btnSign].forEach{
            $0?.cornerRadius = 5.0
        }
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: AddressAuthCellModel,
                   onCopy: (() -> Void)?,
                   onSign: (() -> Void)?
    ){
        self.model = model
        self.onCopy = onCopy
        self.onSign = onSign
        lblAddress.text = model.address
        lblTx.text = "\(model.tx)"
        lblAddress.setStyle(.txtSmaller)
        lblTx.setStyle(.fieldBigger)
        btnSign.isHidden = !model.canSign
    }

    @IBAction func btnCopy(_ sender: Any) {
        onCopy?()
    }

    @IBAction func btnSign(_ sender: Any) {
        onSign?()
    }
}
