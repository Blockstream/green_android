import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class SendBTCConfirmationViewController: KeyboardViewController, SlideButtonDelegate, UITextViewDelegate {

    @IBOutlet var content: SendBTCConfirmationView!
    var uiErrorLabel: UIErrorLabel!
    var wallet: WalletItem?
    var transaction: Transaction!
    var isFiat = false
    var gradientLayer = CAGradientLayer()

    override func viewDidLoad() {
        super.viewDidLoad()
        tabBarController?.tabBar.isHidden = true
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
        title = NSLocalizedString("id_send", comment: "")
        content.fromLabel.text = transaction.isSweep ?  NSLocalizedString("id_sweep_from_paper_wallet", comment: "") : wallet?.localizedName()
        content.slidingButton.delegate = self
        content.slidingButton.buttonText = NSLocalizedString("id_slide_to_send", comment: "")
        uiErrorLabel = UIErrorLabel(self.view)
        content.textView.delegate = self
        content.textView.text = NSLocalizedString("id_add_a_note", comment: "")
        content.textView.textColor = UIColor.customTitaniumLight()
        content.sendingTitle.text = NSLocalizedString("id_sending", comment: "")
        content.fromTitle.text = NSLocalizedString("id_from", comment: "")
        content.toTitle.text = NSLocalizedString("id_to", comment: "")
        content.myNotesTitle.text = NSLocalizedString("id_my_notes", comment: "")
        content.feeTitle.text = NSLocalizedString("id_total_with_fee", comment: "")
        gradientLayer = content.fromView.makeGradientCard()
        content.fromView.layer.insertSublayer(gradientLayer, at: 0)
        setupCurrencyButton()
        update()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        gradientLayer.frame = content.fromView.bounds
        gradientLayer.setNeedsDisplay()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.currencyButton.addTarget(self, action: #selector(click(_:)), for: .touchUpInside)
        content.textView.textColor = UIColor.customTitaniumLight()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.currencyButton.removeTarget(self, action: #selector(click(_:)), for: .touchUpInside)
    }

    func update() {
        let address = transaction.addressees.first!.address
        let satoshi = transaction.addressees.first!.satoshi
        content.toLabel.text = address
        if isFiat {
            content.amountText.text = String.toFiat(satoshi: satoshi, showCurrency: false)
            content.feeLabel.text = String.toFiat(satoshi: satoshi + transaction.fee)
        } else {
            content.amountText.text = String.toBtc(satoshi: satoshi, showDenomination: false)
            content.feeLabel.text = String.toBtc(satoshi: satoshi + transaction.fee)
        }
    }

    func setupCurrencyButton() {
        guard let settings = getGAService().getSettings() else { return }
        if !isFiat {
            content.currencyButton.setTitle(settings.denomination.toString(), for: UIControl.State.normal)
            content.currencyButton.backgroundColor = UIColor.customMatrixGreen()
        } else {
            content.currencyButton.setTitle(settings.getCurrency(), for: UIControl.State.normal)
            content.currencyButton.backgroundColor = UIColor.clear
        }
        content.currencyButton.setTitleColor(UIColor.white, for: UIControl.State.normal)
    }

    @objc func click(_ sender: Any?) {
        isFiat = !isFiat
        setupCurrencyButton()
        update()
    }

    func textViewDidBeginEditing(_ textView: UITextView) {
        if textView.textColor == UIColor.customTitaniumLight() {
            textView.text = nil
            textView.textColor = UIColor.white
        }
    }

    func textViewDidEndEditing(_ textView: UITextView) {
        if textView.text.isEmpty {
            textView.text = NSLocalizedString("id_add_a_note", comment: "")
            textView.textColor = UIColor.customTitaniumLight()
        } else {
            transaction.memo = textView.text
        }
    }

    func completed(slidingButton: SlidingButton) {
        let bgq = DispatchQueue.global(qos: .background)

        firstly {
            uiErrorLabel.isHidden = true
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            signTransaction(transaction: self.transaction)
        }.then(on: bgq) { call in
            call.resolve(self)
        }.map(on: bgq) { resultDict in
            let result = resultDict["result"] as? [String: Any]
            if self.transaction.isSweep {
                let tx = result!["transaction"] as? String
                _ = try getSession().broadcastTransaction(tx_hex: tx!)
                return nil
            } else {
                return try getSession().sendTransaction(details: result!)
            }
        }.then(on: bgq) { (call: TwoFactorCall?) -> Promise<[String: Any]> in
            call?.resolve(self) ?? Promise<[String: Any]> { seal in seal.fulfill([:]) }
        }.done { _ in
            self.executeOnDone()
        }.catch { error in
            self.stopAnimating()
            self.content.slidingButton.reset()
            self.uiErrorLabel.isHidden = false
            if let twofaError = error as? TwoFactorCallError {
                switch twofaError {
                case .failure(let localizedDescription), .cancel(let localizedDescription):
                    self.uiErrorLabel.text = localizedDescription
                }
            } else {
                self.uiErrorLabel.text = error.localizedDescription
            }
        }
    }

    func executeOnDone() {
        self.startAnimating(message: NSLocalizedString("id_transaction_sent", comment: ""))
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 1.1) {
            self.stopAnimating()
            self.popBack(toControllerType: TransactionsController.self)
        }
    }

    override func keyboardWillShow(notification: NSNotification) {
        // Modified slightly for use in Green from the public release at "Managing the Keyboard" from Text Programming Guide for iOS
        super.keyboardWillShow(notification: notification)
        if let kbSize = (notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue {
            let contentInsets = UIEdgeInsets(top: 0.0, left: 0.0, bottom: kbSize.height, right: 0.0)
            content.scrollView.contentInset = contentInsets
            content.scrollView.scrollIndicatorInsets = contentInsets
            var aRect = self.view.frame
            aRect.size.height -= kbSize.height
            if !aRect.contains(content.textView.frame.origin) {
                content.scrollView.scrollRectToVisible(aRect, animated: true)
            }
        }
    }

    override func keyboardWillHide(notification: NSNotification) {
        super.keyboardWillHide(notification: notification)
        let contentInsets = UIEdgeInsets.zero
        content.scrollView.contentInset = contentInsets
        content.scrollView.scrollIndicatorInsets = contentInsets
    }

    /// pop back to specific viewcontroller
    func popBack<T: UIViewController>(toControllerType: T.Type) {
        if var viewControllers: [UIViewController] = self.navigationController?.viewControllers {
            viewControllers = viewControllers.reversed()
            for currentViewController in viewControllers {
                if currentViewController .isKind(of: toControllerType) {
                    self.navigationController?.popToViewController(currentViewController, animated: true)
                    break
                }
            }
        }
    }
}

@IBDesignable
class SendBTCConfirmationView: UIView {
    @IBOutlet weak var textView: UITextView!
    @IBOutlet weak var slidingButton: SlidingButton!
    @IBOutlet weak var feeLabel: UILabel!
    @IBOutlet weak var fromLabel: UILabel!
    @IBOutlet weak var fromView: UIView!
    @IBOutlet weak var toLabel: UILabel!
    @IBOutlet weak var sendingTitle: UILabel!
    @IBOutlet weak var fromTitle: UILabel!
    @IBOutlet weak var toTitle: UILabel!
    @IBOutlet weak var myNotesTitle: UILabel!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var feeTitle: UILabel!
    @IBOutlet weak var amountText: UITextField!
    @IBOutlet weak var currencyButton: UIButton!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }
}
