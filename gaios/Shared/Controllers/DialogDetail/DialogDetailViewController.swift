import Foundation
import UIKit

import gdk

enum DetailCellType: CaseIterable {
    case name
    case identifier
    case amount
    case precision
    case ticker
    case issuer
}

class DialogDetailViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var tableViewHeight: NSLayoutConstraint!

    var tag: String!
    var asset: AssetInfo?
    var satoshi: Int64?
    private var assetDetailCellTypes = DetailCellType.allCases
    private var isLBTC: Bool {
        get {
            return tag == GdkNetworks.shared.liquidSS.policyAsset
        }
    }
    private var assetsUpdatedToken: NSObjectProtocol?
    var obs: NSKeyValueObservation?

    private var hideBalance: Bool {
        return UserDefaults.standard.bool(forKey: AppStorage.hideBalance)
    }

    lazy var blurredView: UIView = {
        let containerView = UIView()
        let blurEffect = UIBlurEffect(style: .dark)
        let customBlurEffectView = CustomVisualEffectView(effect: blurEffect, intensity: 0.4)
        customBlurEffectView.frame = self.view.bounds

        let dimmedView = UIView()
        dimmedView.backgroundColor = .black.withAlphaComponent(0.3)
        dimmedView.frame = self.view.bounds
        containerView.addSubview(customBlurEffectView)
        containerView.addSubview(dimmedView)
        return containerView
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        register()
        setContent()
        setStyle()

        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)

        view.alpha = 0.0
        anchorBottom.constant = -cardView.frame.size.height

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTap))
            tappableBg.addGestureRecognizer(tapToClose)

        obs = tableView.observe(\UITableView.contentSize, options: .new) { [weak self] table, _ in
            self?.tableViewHeight.constant = table.contentSize.height
        }

        AnalyticsManager.shared.recordView(.assetDetails)
    }

    deinit {
        print("deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        anchorBottom.constant = 0
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
            self.view.layoutIfNeeded()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = assetsUpdatedToken {
            NotificationCenter.default.removeObserver(token)
            assetsUpdatedToken = nil
        }

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if isLBTC { assetDetailCellTypes.remove(at: 1) }
        assetsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil, queue: .main, using: onAssetsUpdated)
    }

    @objc func didTap(gesture: UIGestureRecognizer) {

        dismiss()
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_asset_details", comment: "")
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5
        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
    }

    func register() {
        ["DialogDetailCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
    }

    func dismiss() {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
        })
    }

    @objc func didSwipe(gesture: UIGestureRecognizer) {

        if let swipeGesture = gesture as? UISwipeGestureRecognizer {
            switch swipeGesture.direction {
            case .down:
                dismiss()
            default:
                break
            }
        }
    }

    func onAssetsUpdated(_ notification: Notification) {
        self.tableView.reloadData()
    }
}

extension DialogDetailViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return assetDetailCellTypes.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: DialogDetailCell.identifier) as? DialogDetailCell {
            cell.selectionStyle = .none
            let cellType = assetDetailCellTypes[indexPath.row]
            let assetName = asset?.name == "" ? nil : asset?.name
            let assetTicker = asset?.ticker == "" ? nil : asset?.ticker
            switch cellType {
            case .name:
                cell.configure(NSLocalizedString("id_asset_name", comment: ""), assetName ?? NSLocalizedString("id_no_registered_name_for_this", comment: ""))
            case .identifier:
                cell.configure(NSLocalizedString("id_asset_id", comment: ""), tag)
            case .amount:
                let balance = Balance.fromSatoshi(satoshi ?? 0, assetId: asset!.assetId)
                cell.configureAmount(NSLocalizedString("id_total_balance", comment: ""), balance?.toValue().0 ?? "", hideBalance)
            case .precision:
                cell.configure(NSLocalizedString("id_precision", comment: ""), isLBTC ? "8" : String(asset?.precision ?? 0))
            case .ticker:
                cell.configure(NSLocalizedString("id_ticker", comment: ""), assetTicker ?? NSLocalizedString("id_no_registered_ticker_for_this", comment: ""))
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
