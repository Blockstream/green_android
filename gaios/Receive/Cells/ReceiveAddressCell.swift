import UIKit

class ReceiveAddressCell: UITableViewCell {

    @IBOutlet weak var bgCard: UIView!
    @IBOutlet weak var bgCardQR: UIView!
    @IBOutlet weak var btnQRCode: UIButton!
    @IBOutlet weak var lblAddress: UILabel!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var qrFrame: UIView!
    @IBOutlet weak var loader: UIActivityIndicatorView!
    
    @IBOutlet weak var lnBannerBox: UIView!
    @IBOutlet weak var lnBanner: UIView!
    @IBOutlet weak var lblInfo: UILabel!
    @IBOutlet weak var btnRefresh: UIButton!
    
    static var identifier: String { return String(describing: self) }

    var onCopyToClipboard: (() -> Void)?
    var onRefreshClick: (() -> Void)?
    var onLongpress: (() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()

        bgCard.layer.cornerRadius = 5.0
        //btnCopy.setTitle("id_copy_address".localized, for: .normal)
        btnCopy.cornerRadius = 5.0

        let longPressRecognizer = UILongPressGestureRecognizer(target: self, action: #selector(longPressed))
        qrFrame.addGestureRecognizer(longPressRecognizer)
        lnBanner.backgroundColor = UIColor.gGreenFluo().withAlphaComponent(0.2)
        lnBanner.cornerRadius = 5.0
        lblInfo.setStyle(.txtSmaller)
        lnBannerBox.isHidden = true
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: ReceiveAddressCellModel,
                   isAnimating: Bool,
                   onCopyToClipboard: (() -> Void)?,
                   onRefreshClick: (() -> Void)?,
                   onLongpress: (() -> Void)? = nil
    ) {
        lblAddress.text = model.text
        self.onCopyToClipboard = onCopyToClipboard
        self.onRefreshClick = onRefreshClick
        if let uri = model.text {
            let dim = min(qrFrame.frame.size.width, qrFrame.frame.size.height)
            let frame = CGRect(x: 0.0, y: 0.0, width: dim, height: dim)
            btnQRCode.setImage(QRImageGenerator.imageForTextWhite(text: uri, frame: frame), for: .normal)
            btnQRCode.imageView?.contentMode = .scaleAspectFit
            if model.type == .bolt11 {
                qrFrame.backgroundColor = .white
            }
        } else {
            btnQRCode.setImage(UIImage(), for: .normal)
            qrFrame.backgroundColor = .clear
        }
        if isAnimating {
            loader.startAnimating()
            loader.isHidden = false
        } else {
            loader.stopAnimating()
            loader.isHidden = true
        }
        lnBannerBox.isHidden = true
        if let onChaininfo = model.onChaininfo, model.type == .swap {
            lnBannerBox.isHidden = false
            lblInfo.text = onChaininfo
            btnRefresh.isHidden = true
        }
        
        self.onLongpress = onLongpress
    }

    @objc func longPressed(sender: UILongPressGestureRecognizer) {

        if sender.state == UIGestureRecognizer.State.began {
            onLongpress?()
        }
    }

    @IBAction func copyAction(_ sender: Any) {
        onCopyToClipboard?()
    }
    
    @IBAction func refreshClick(_ sender: Any) {
        onRefreshClick?()
    }
}
