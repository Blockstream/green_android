import Foundation
import UIKit
import PromiseKit

protocol DialogAmountViewControllerDelegate: AnyObject {
    func didConfirm(satoshi: Int64?)
    func didCancel()
}

enum DialogAmountAction {
    case confirm
    case cancel
}

class DialogAmountViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var amountTextField: UITextField!
    @IBOutlet weak var lblDenom: UILabel!
    @IBOutlet weak var btnFiat: UIButton!
    @IBOutlet weak var btnClear: UIButton!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var btnsStack: UIStackView!
    @IBOutlet weak var bgField: UIView!

    var selectedType = TransactionBaseType.BTC
    var prefill: Int64?
    var wallet: WalletItem?
    var buttonConstraint: NSLayoutConstraint?

    weak var delegate: DialogAmountViewControllerDelegate?

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

        amountTextField.attributedPlaceholder = NSAttributedString(string: "0.00".localeFormattedString(2), attributes: [NSAttributedString.Key.foregroundColor: UIColor.white])
        if let satoshi = prefill {
            if let (amount, _) = Balance.fromSatoshi(satoshi, assetId: wallet!.gdkNetwork.getFeeAsset())?.toValue() {
                amountTextField.text = "\(amount)"
            }
        }

        setContent()
        setStyle()

        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)

        view.alpha = 0.0
        anchorBottom.constant = -200

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTap))
            tappableBg.addGestureRecognizer(tapToClose)

        AnalyticsManager.shared.recordView(.requestAmount, sgmt: AnalyticsManager.shared.subAccSeg(AccountsRepository.shared.current, walletType: wallet?.type))
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
        amountTextField.becomeFirstResponder()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reload()
    }

    func reload() {
        getSatoshi() != nil ? btnConfirm.setStyle(.primary) : btnConfirm.setStyle(.primaryDisabled)
        setDenomination()
        updateEstimate()
    }

    func updateEstimate() {
        let satoshi = getSatoshi() ?? 0
        let assetId = wallet!.gdkNetwork.getFeeAsset()
        if selectedType == TransactionBaseType.BTC {
            let (amount, denom) = Balance.fromSatoshi(satoshi, assetId: assetId)?.toFiat() ?? ("", "")
            lblHint.text = "≈ \(amount) \(denom)"
        } else {
            let (amount, denom) = Balance.fromSatoshi(satoshi, assetId: assetId)?.toValue() ?? ("", "")
            lblHint.text = "≈ \(amount) \(denom)"
        }
    }

    func setDenomination() {
        guard let session = WalletManager.current?.prominentSession,
                let settings = session.settings else {
            return
        }
        if selectedType == TransactionBaseType.BTC {
            let string = settings.denomination.string(for: session.gdkNetwork)
            lblDenom.text = string
//            btnFiat.setTitle(string, for: UIControl.State.normal)
//            btnFiat.backgroundColor = UIColor.customMatrixGreen()
//            btnFiat.setTitleColor(UIColor.white, for: UIControl.State.normal)
        } else {
            let isMainnet = AccountsRepository.shared.current?.gdkNetwork?.mainnet ?? true
            lblDenom.text = isMainnet ? settings.getCurrency() : "FIAT"
//            btnFiat.setTitle(isMainnet ? settings.getCurrency() : "FIAT", for: UIControl.State.normal)
//            btnFiat.backgroundColor = UIColor.clear
//            btnFiat.setTitleColor(UIColor.white, for: UIControl.State.normal)
        }
    }

    @objc func didTap(gesture: UIGestureRecognizer) {

        dismiss(.cancel)
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.btnsStack.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height - 14.0)
            self.buttonConstraint?.isActive = true
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
        })
    }

    func setContent() {
        lblTitle.text = "id_request_amount".localized
        lblHint.text = ""
        btnConfirm.setTitle("Confirm", for: .normal)
        amountTextField.attributedPlaceholder = NSAttributedString(
            string: "id_amount".localized,
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5
        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        amountTextField.font = UIFont.systemFont(ofSize: 20.0, weight: .medium)
        lblDenom.font = UIFont.systemFont(ofSize: 14.0, weight: .medium)
        lblHint.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
        lblHint.textColor = .white.withAlphaComponent(0.4)
        bgField.layer.cornerRadius = 5.0
    }

    func getSatoshi() -> Int64? {
        var amountText = amountTextField.text!
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        if selectedType == TransactionBaseType.BTC {
            let assetId = wallet!.gdkNetwork.getFeeAsset()
            return Balance.fromDenomination(amountText, assetId: assetId)?.satoshi
        } else {
            return Balance.fromFiat(amountText)?.satoshi
        }
    }

    func dismiss(_ action: DialogAmountAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .confirm:
                self.delegate?.didConfirm(satoshi: self.getSatoshi())
            case .cancel:
                self.delegate?.didCancel()
            }
        })
    }

    @objc func didSwipe(gesture: UIGestureRecognizer) {

        if let swipeGesture = gesture as? UISwipeGestureRecognizer {
            switch swipeGesture.direction {
            case .down:
                dismiss(.cancel)
            default:
                break
            }
        }
    }

    @IBAction func amountDidChange(_ sender: Any) {
        reload()
    }

    @IBAction func btnFiat(_ sender: Any) {
        let satoshi = getSatoshi() ?? 0
        let assetId = wallet!.gdkNetwork.getFeeAsset()
        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId) {
            let (amount, _) = selectedType == TransactionBaseType.BTC ? balance.toDenom() : balance.toFiat()
            if amount.isEmpty {
                showError(NSLocalizedString("id_your_favourite_exchange_rate_is", comment: ""))
                return
            }
        }
        if selectedType == TransactionBaseType.BTC {
            selectedType = TransactionBaseType.FIAT
        } else {
            selectedType = TransactionBaseType.BTC
        }
        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId) {
            let (amountStr, _) = selectedType == TransactionBaseType.BTC ? balance.toDenom() : balance.toFiat()
            let amount = Double(amountStr)
            amountTextField.text = amountStr
            if amount == 0.0 {
                amountTextField.text = ""
            }
        }
        reload()
    }

    @IBAction func btnClear(_ sender: Any) {
        amountTextField.text = ""
    }

    @IBAction func btnConfirm(_ sender: Any) {
        dismiss(.confirm)
    }
}
