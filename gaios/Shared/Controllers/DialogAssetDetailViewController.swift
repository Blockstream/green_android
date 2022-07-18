import Foundation
import UIKit
import PromiseKit

enum DetailCellType: CaseIterable {
    case name
    case identifier
    case amount
    case precision
    case ticker
    case issuer
}

class DialogAssetDetailViewController: UIViewController {

    var tag: String!
    var asset: AssetInfo?
    var satoshi: UInt64?
    private var assetDetailCellTypes = DetailCellType.allCases
    private var isLBTC: Bool {
        get {
            return tag == getGdkNetwork("liquid").policyAsset
        }
    }
    private var assetsUpdatedToken: NSObjectProtocol?
    var obs: NSKeyValueObservation?

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var tableViewHeight: NSLayoutConstraint!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
        obs = tableView.observe(\UITableView.contentSize, options: .new) { [weak self] table, _ in
            self?.tableViewHeight.constant = table.contentSize.height
        }

        AnalyticsManager.shared.recordView(.assetDetails)
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_asset_details", comment: "")
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if isLBTC { assetDetailCellTypes.remove(at: 1) }
        assetsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil, queue: .main, using: onAssetsUpdated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = assetsUpdatedToken {
            NotificationCenter.default.removeObserver(token)
            assetsUpdatedToken = nil
        }
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func dismiss(_ value: MnemonicLengthOption?) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
        })
    }

    func onAssetsUpdated(_ notification: Notification) {
        self.tableView.reloadData()
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(nil)
    }

}

extension DialogAssetDetailViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return assetDetailCellTypes.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "AssetDetailCell") as? AssetDetailCell {
            cell.selectionStyle = .none
            let cellType = assetDetailCellTypes[indexPath.row]
            switch cellType {
            case .name:
                cell.configure(NSLocalizedString("id_asset_name", comment: ""), asset?.name ?? NSLocalizedString("id_no_registered_name_for_this", comment: ""))
            case .identifier:
                cell.configure(NSLocalizedString("id_asset_id", comment: ""), tag)
            case .amount:
                let balance = Balance.convert(details: ["satoshi": satoshi ?? 0, "asset_info": asset!.encode()])
                cell.configure(NSLocalizedString("id_total_balance", comment: ""), balance?.get(tag: tag).0 ?? "")
            case .precision:
                cell.configure(NSLocalizedString("id_precision", comment: ""), isLBTC ? "8" : String(asset?.precision ?? 0))
            case .ticker:
                cell.configure(NSLocalizedString("id_ticker", comment: ""), isLBTC ? "L-BTC" : asset?.ticker ?? NSLocalizedString("id_no_registered_ticker_for_this", comment: ""))
            case .issuer:
                cell.configure(NSLocalizedString("id_issuer", comment: ""), isLBTC ? NSLocalizedString("id_lbtc_has_no_issuer_and_is", comment: "") : asset?.entity?.domain ?? NSLocalizedString("id_unknown", comment: ""))
            }
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if assetDetailCellTypes[indexPath.row] == .issuer && isLBTC {
            if let url = URL(string: "https://docs.blockstream.com/liquid/technical_overview.html#watchmen") {
                if UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url, options: [:], completionHandler: nil)
                }
            }
        }
    }
}
