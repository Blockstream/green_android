import UIKit

struct QRDialogInfo {
    var item: String
    var title: String
    var hint: String
}

class WatchOnlyMultisigSettingsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var bg: UIView!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.textColor = .white
        lblHint.textColor = UIColor.customGrayLight()
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: WatchOnlySettingsCellModel? {
        didSet {
            lblTitle.text = viewModel?.title
            lblHint.text = viewModel?.subtitle
        }
    }
}

class WatchOnlySinglesigSettingsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnQR: UIButton!

    var onCopy: ((String) -> Void)?
    var onQR: ((QRDialogInfo) -> Void)?

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.textColor = .white
        lblHint.textColor = UIColor.customGrayLight()
        bg.layer.cornerRadius = 5.0
        btnCopy.setImage(UIImage(named: "ic_copy_new")?.maskWithColor(color: UIColor.gGreenMatrix()), for: .normal)
        btnQR.setImage(UIImage(named: "qr")?.maskWithColor(color: UIColor.gGreenMatrix()), for: .normal)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: WatchOnlySettingsCellModel? {
        didSet {
            lblTitle.text = viewModel?.title
            lblHint.text = viewModel?.subtitle
        }
    }

    func configure(viewModel: WatchOnlySettingsCellModel?,
                   onCopy: ((String) -> Void)?,
                   onQR: ((QRDialogInfo) -> Void)?
    ) {
        self.viewModel = viewModel
        self.onCopy = onCopy
        self.onQR = onQR
    }

    @IBAction func btnCopy(_ sender: Any) {
        onCopy?(viewModel?.subtitle ?? "")
    }

    @IBAction func btnQR(_ sender: Any) {
        let qrInfo = QRDialogInfo(item: viewModel?.subtitle ?? "",
                                  title: viewModel?.isExtended == true ? "Extended Public Key" : "Output Descriptor",
                                  hint: viewModel?.title ?? "")
        onQR?(qrInfo)
    }
}

class WatchOnlyHeaderSettingsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var bg: UIView!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.textColor = .white
        lblHint.textColor = UIColor.customGrayLight()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: WatchOnlySettingsCellModel? {
        didSet {
            lblTitle.text = viewModel?.title
            lblHint.text = viewModel?.subtitle
        }
    }
}
