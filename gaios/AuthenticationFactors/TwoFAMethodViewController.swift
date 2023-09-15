import Foundation
import UIKit
import gdk

enum TwoFAMethodAction {
    case cancel
    case type(value: TwoFactorType)
}

class TwoFAMethodViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!

    @IBOutlet weak var lblSms: UILabel!
    @IBOutlet weak var lblCall: UILabel!
    @IBOutlet weak var lblEmail: UILabel!
    @IBOutlet weak var lblGauth: UILabel!
    @IBOutlet weak var btnCancel: UIButton!

    @IBOutlet weak var cardSms: UIView!
    @IBOutlet weak var cardCall: UIView!
    @IBOutlet weak var cardEmail: UIView!
    @IBOutlet weak var cardGauth: UIView!
    
    @IBOutlet var btns: [UIButton]!
    
    var onCancel: (() -> Void)?
    var onType: ((TwoFactorType) -> Void)?
    var methods: [String] = []

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
        cardSms.isHidden = !methods.contains(TwoFactorType.sms.rawValue)
        cardCall.isHidden = !methods.contains(TwoFactorType.phone.rawValue)
        cardEmail.isHidden = !methods.contains(TwoFactorType.email.rawValue)
        cardGauth.isHidden = !methods.contains(TwoFactorType.gauth.rawValue)
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

        lblSms.text = "id_sms".localized
        lblCall.text = "id_call".localized
        lblEmail.text = "id_email".localized
        lblGauth.text = "id_authenticator_app".localized
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        cardView.borderWidth = 1.0
        cardView.borderColor = .white.withAlphaComponent(0.05)
        lblTitle.setStyle(.txtBigger)
        btnCancel.setStyle(.inline)

        [lblSms, lblCall, lblEmail, lblGauth].forEach {
            $0.setStyle(.txtBigger)
        }
        btns.forEach{
            $0.isUserInteractionEnabled = false
            $0.backgroundColor = UIColor.gGreenMatrix()
            $0.cornerRadius = 4.0
        }
        [cardSms, cardCall, cardEmail, cardGauth].forEach{
            $0?.cornerRadius = 5.0
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
    
    func onTap(_ type: TwoFactorType) {
        UIView.animate(withDuration: 0.2, animations: {
            switch type {
            case .sms:
                self.cardSms.alpha = 0.7
            case .phone:
                self.cardCall.alpha = 0.7
            case .email:
                self.cardEmail.alpha = 0.7
            case .gauth:
                self.cardGauth.alpha = 0.7
            }
        }, completion: { _ in
            self.dismiss(.type(value: type))
        })
    }
    @IBAction func btnSMS(_ sender: Any) {
        onTap(.sms)
    }
    @IBAction func btnCall(_ sender: Any) {
        onTap(.phone)
    }
    @IBAction func btnEmail(_ sender: Any) {
        onTap(.email)
    }
    @IBAction func btnGauth(_ sender: Any) {
        onTap(.gauth)
    }
    @IBAction func btnCancel(_ sender: Any) {
        dismiss(.cancel)
    }
}
