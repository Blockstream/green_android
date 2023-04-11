import Foundation
import UIKit
import PromiseKit

protocol DialogRenameViewControllerDelegate: AnyObject {
    func didRename(name: String, index: String?)
    func didCancel()
}

enum RenameAction {
    case save
    case cancel
}

class DialogRenameViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var nameTextField: UITextField!
    @IBOutlet weak var btnSave: UIButton!

    var buttonConstraint: NSLayoutConstraint?

    weak var delegate: DialogRenameViewControllerDelegate?

    var index: String?
    var isAccountRename = false
    var prefill: String = ""

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

        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)

        view.alpha = 0.0
        anchorBottom.constant = -cardView.frame.size.height

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTap))
            tappableBg.addGestureRecognizer(tapToClose)

        nameTextField.placeholder = ""
        nameTextField.text = prefill
        updateUI()

        if isAccountRename {
            AnalyticsManager.shared.recordView(.renameAccount, sgmt: AnalyticsManager.shared.sessSgmt(AccountsRepository.shared.current))
        } else {
            AnalyticsManager.shared.recordView(.renameWallet)
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
        nameTextField.becomeFirstResponder()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func updateUI() {
        if nameTextField.text?.count ?? 0 > 2 {
            btnSave.setStyle(.primary)
        } else {
            btnSave.setStyle(.primaryDisabled)
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
            self.buttonConstraint = self.btnSave.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height - 14.0)
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
        lblTitle.text = NSLocalizedString("id_rename_wallet", comment: "")
        if isAccountRename {
            lblTitle.text = NSLocalizedString("id_rename_account", comment: "")
        }
        btnSave.setTitle(NSLocalizedString("id_submit", comment: ""), for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5
        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        nameTextField.layer.cornerRadius = 5.0
        nameTextField.setLeftPaddingPoints(15.0)
        nameTextField.setRightPaddingPoints(15.0)
        nameTextField.leftViewMode = .always
    }

    func dismiss(_ action: RenameAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                self.delegate?.didCancel()
            case .save:
                self.delegate?.didRename(name: self.nameTextField.text ?? "", index: self.index)
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

    @IBAction func nameDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnSave(_ sender: Any) {
        dismiss(.save)
    }
}
