import Foundation
import UIKit
import gdk

protocol DialogNodeViewControllerProtocol {
    func onCloseChannels()
}

class DialogNodeViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var tableViewHeight: NSLayoutConstraint!

    var viewModel: DialogNodeViewModel!
    var delegate: DialogNodeViewControllerProtocol?
    private var nodeCellTypes: [NodeCellType] { viewModel.cells }
    private var obs: NSKeyValueObservation?

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
//        if let token = assetsUpdatedToken {
//            NotificationCenter.default.removeObserver(token)
//            assetsUpdatedToken = nil
//        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    @objc func didTap(gesture: UIGestureRecognizer) {

        dismiss()
    }

    func setContent() {
        lblTitle.text = "Node Info"
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

extension DialogNodeViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return nodeCellTypes.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: DialogDetailCell.identifier) as? DialogDetailCell {
            cell.selectionStyle = .none
            let cellType = nodeCellTypes[indexPath.row]

            switch cellType {
            case .id:
                cell.configure("ID", viewModel.id)
            case .channelsBalance:
                cell.configureAmount("id_account_balance".localized, viewModel.channelsBalance, hideBalance)
            case .inboundLiquidity:
                cell.configureAmount("Inbound Liquidity".localized, viewModel.inboundLiquidity, hideBalance)
            case .maxPayble:
                cell.configureAmount("Max Payable Amount".localized, viewModel.maxPayble, hideBalance)
            case .maxSinglePaymentAmount:
                cell.configureAmount("Max Single Payment Amount".localized, viewModel.maxSinglePaymentAmount, hideBalance)
            case .maxReceivable:
                cell.configureAmount("Max Receivable Amount".localized, viewModel.maxReceivable, hideBalance)
            case .connectedPeers:
                cell.configureAmount("Connected Peers".localized, viewModel.connectedPeers, false)
            case .closeChannels:
                cell.configureAmount("Close Channels".localized, viewModel.closeChannels, false)
            }
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let cellType = nodeCellTypes[indexPath.row]
        switch cellType {
        case .closeChannels:
            delegate?.onCloseChannels()
            dismiss()
        default:
            break
        }
    }
}
