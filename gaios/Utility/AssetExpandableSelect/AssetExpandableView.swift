import UIKit

class AssetExpandableView: UIView {
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var tapView: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var ampTip: UIView!
    @IBOutlet weak var lblAmp: UILabel!
    @IBOutlet weak var createNew: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var lblTitle: UILabel!

    var reload: (() -> Void)?
    var onCreate: (() -> Void)?

    func configure(model: AssetSelectCellModel,
                   hasAccounts: Bool,
                   open: Bool,
                   onCreate: (() -> Void)?
    ) {
        let name = model.asset?.name ?? model.asset?.assetId
        title.text = name
        icon?.image = model.icon

        ampTip.cornerRadius = 5.0
        lblAmp.text = String(format: "id_s_is_a_liquid_asset_you_can".localized, name ?? "")
        ampTip.isHidden = !(model.asset?.amp ?? false && open)

        createNew.isHidden = true
        if open {
            bg.borderWidth = 2.0
            bg.borderColor = UIColor.gGreenMatrix()
            bg.layer.cornerRadius = 5
            bg.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
            // always closed, added action in section footer
            createNew.isHidden = true // hasAccounts
        } else {
            bg.borderWidth = 0.0
        }
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
        lblTitle.text = "id_create_new_account".localized
        self.onCreate = onCreate
    }

    @IBAction func btnOnCreate(_ sender: Any) {
        onCreate?()
    }
}
