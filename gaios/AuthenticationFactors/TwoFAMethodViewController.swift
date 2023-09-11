import Foundation
import UIKit
import gdk

enum TwoFAMethodOption {
    case undefined
    case sms
    case call
}

enum TwoFAMethodAction {
    case cancel
    case type(value: TwoFactorType)
}

class TwoFAMethodViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnSMS: UIButton!
    @IBOutlet weak var btnCall: UIButton!
    @IBOutlet weak var btnEmail: UIButton!
    @IBOutlet weak var btnGAuth: UIButton!
    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnOk: UIButton!

    var onCancel: (() -> Void)?
    var onType: ((TwoFactorType) -> Void)?
    var tfType: TwoFactorType?
    var methods: [String] = []

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
        btnSMS.isHidden = !methods.contains(TwoFactorType.sms.rawValue)
        btnCall.isHidden = !methods.contains(TwoFactorType.phone.rawValue)
        btnEmail.isHidden = !methods.contains(TwoFactorType.email.rawValue)
        btnGAuth.isHidden = !methods.contains(TwoFactorType.gauth.rawValue)
        refresh()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func setContent() {
        lblTitle.text = "id_choose_method_to_authorize_the".localized
        btnCancel.setTitle("id_cancel".localized, for: .normal)
        btnOk.setTitle("id_ok".localized, for: .normal)
        btnSMS.setTitle("id_sms".localized, for: .normal)
        btnCall.setTitle("id_call".localized, for: .normal)
        btnEmail.setTitle("id_email".localized, for: .normal)
        btnGAuth.setTitle("id_authenticator_app".localized, for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.setStyle(.txtBigger)
        btnCancel.setStyle(.inline)
        btnOk.setStyle(.inline)
        [btnSMS, btnCall, btnEmail, btnGAuth].forEach {
            $0.setStyle(.inlineGray)
        }
    }

    func refresh() {
        let uImg = UIImage(named: "unselected_circle")!
        let sImg = UIImage(named: "selected_circle")!
        [btnSMS, btnCall, btnEmail, btnGAuth].forEach{
            $0.setImage(uImg, for: .normal)
        }
        btnOk.setStyle(.inlineDisabled)
        switch tfType {
        case .sms:
            btnSMS.setImage(sImg, for: .normal)
            btnOk.setStyle(.inline)
        case .phone:
            btnCall.setImage(sImg, for: .normal)
            btnOk.setStyle(.inline)
        case .email:
            btnEmail.setImage(sImg, for: .normal)
            btnOk.setStyle(.inline)
        case .gauth:
            btnGAuth.setImage(sImg, for: .normal)
            btnOk.setStyle(.inline)
        case .none:
            break
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss(_ action: TwoFAMethodAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                switch action {
                case .cancel:
                    self.onCancel?()
                case .type(let value):
                    self.onType?(value)
                }
            })
        })
    }
    @IBAction func btnSMS(_ sender: Any) {
        tfType = .sms
        refresh()
    }
    
    @IBAction func btnCall(_ sender: Any) {
        tfType = .phone
        refresh()
    }
    @IBAction func btnEmail(_ sender: Any) {
        tfType = .email
        refresh()
    }
    
    @IBAction func btnGauth(_ sender: Any) {
        tfType = .gauth
        refresh()
    }
    

    @IBAction func btnCancel(_ sender: Any) {
        dismiss(.cancel)
    }
    
    @IBAction func btnOk(_ sender: Any) {
        guard let value = tfType else { return }
        dismiss(.type(value: value))
    }
}
