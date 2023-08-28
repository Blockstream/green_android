import Foundation
import UIKit

class DialogSignViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblAddress: UILabel!
    @IBOutlet weak var messageTextView: UITextView!
    @IBOutlet weak var btnPaste: UIButton!
    @IBOutlet weak var btnSign: UIButton!
    @IBOutlet weak var lblSign: UILabel!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnsStack: UIStackView!
    @IBOutlet weak var stackBottom: NSLayoutConstraint!
    @IBOutlet weak var signView: UIView!
    @IBOutlet weak var signMessageView: UIView!
    @IBOutlet weak var lblSignMessage: UILabel!
    @IBOutlet weak var animateView: UIView!
    
    
    var viewModel: DialogSignViewModel!
    var dialogJadeCheckViewController: DialogJadeCheckViewController?

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

        setContent()
        setStyle()

        lblAddress.text = viewModel.address
        signView.isHidden = true

        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)

        view.alpha = 0.0
        anchorBottom.constant = -cardView.frame.size.height

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTap))
            tappableBg.addGestureRecognizer(tapToClose)

        messageTextView.delegate = self
        refreshUI()
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

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        refreshUI()
    }

    @objc func didTap(gesture: UIGestureRecognizer) {

        dismiss()
    }

    func setContent() {
        lblTitle.text = "Authenticate Address"
        btnPaste.setTitle("id_paste".localized, for: .normal)
        btnSign.setTitle("Sign message", for: .normal)
        btnCopy.setTitle("Copy Signature", for: .normal)
        lblSign.text = ""
        lblSignMessage.text = "This is the signature for the message signed by the address for proof of ownership.".localized
    }

    func setStyle() {
        btnSign.setStyle(.primaryDisabled)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        messageTextView.cornerRadius = 5.0
        [btnPaste, btnCopy].forEach{
            $0?.cornerRadius = 5.0
        }
        lblAddress.setStyle(.txtCard)
        lblSignMessage.setStyle(.subTitle)
    }

    func refreshUI() {

        if messageTextView.text.count > 3 {
            btnSign.setStyle(.primary)
        } else {
            btnSign.setStyle(.primaryDisabled)
        }
        signMessageView.isHidden = true
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)

        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.stackBottom.constant = keyboardFrame.height
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillHide(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.stackBottom.constant = 36.0
        })
    }

    func onSignatureReady(_ signature: String?) {
        lblSign.text = signature
        btnPaste.isHidden = true
        btnSign.isHidden = true
        signMessageView.isHidden = false
        messageTextView.isEditable = false

        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.3) {
            let riveView = RiveModel.animationCheckMark.createRiveView()
            riveView.frame = CGRect(x: 0.0, y: 0.0, width: self.animateView.frame.width, height: self.animateView.frame.height)
            
            print(riveView.frame)
            self.animateView.addSubview(riveView)
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

    @IBAction func btnPaste(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            messageTextView.text = txt
            refreshUI()
        }
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }
    
    @IBAction func btnSign(_ sender: Any) {
        signView.isHidden = false
        messageTextView.endEditing(true)
        lblSign.text = ""
        let message = messageTextView.text
        Task {
            do {
                if viewModel.isHW {
                    showHWCheckDialog(message: message ?? "")                    
                }
                let signature = try await viewModel.sign(message: message ?? "")
                hideHWCheckDialog()
                await MainActor.run {
                    onSignatureReady(signature)
                }
            } catch {
                hideHWCheckDialog()
                showError(error)
            }
        }
    }
    
    @MainActor
    func showHWCheckDialog(message: String) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        dialogJadeCheckViewController = storyboard.instantiateViewController(withIdentifier: "DialogJadeCheckViewController") as? DialogJadeCheckViewController
        if let vc = dialogJadeCheckViewController {
            vc.isLedger = BleViewModel.shared.type == .Ledger
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @MainActor
    func hideHWCheckDialog() {
        dialogJadeCheckViewController?.dismiss()
    }
    
    @IBAction func btnCopy(_ sender: Any) {
        if let sign = lblSign.text {
            UIPasteboard.general.string = sign
            DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 2.0)
        }
    }
}

extension DialogSignViewController: UITextViewDelegate {

    func textViewDidBeginEditing(_ textView: UITextView) {
        signView.isHidden = true
    }
    func textViewDidChange(_ textView: UITextView) {
        refreshUI()
    }
}
