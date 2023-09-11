import Foundation
import UIKit

protocol TwoFAMethodViewControllerDelegate: AnyObject {
    func onDone()
}

enum TwoFAMethodOption {
    case undefined
    case sms
    case call
}

class TwoFAMethodViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnSMS: UIButton!
    @IBOutlet weak var btnCall: UIButton!
    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnOk: UIButton!
    
    weak var delegate: TwoFAMethodViewControllerDelegate?

    var selectedOption: TwoFAMethodOption = .undefined

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
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
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.setStyle(.txtBigger)
        btnCancel.setStyle(.inline)
        btnOk.setStyle(.inline)
        btnSMS.setStyle(.inlineGray)
        btnCall.setStyle(.inlineGray)
    }

    func refresh() {
        let uImg = UIImage(named: "unselected_circle")!
        let sImg = UIImage(named: "selected_circle")!
        [btnSMS, btnCall].forEach{
            $0.setImage(uImg, for: .normal)
        }
        btnOk.setStyle(.inlineDisabled)
        switch selectedOption {
        case .undefined:
            break
        case .sms:
            btnSMS.setImage(sImg, for: .normal)
            btnOk.setStyle(.inline)
        case .call:
            btnCall.setImage(sImg, for: .normal)
            btnOk.setStyle(.inline)
        }    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss() {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                self.delegate?.onDone()
            })
        })
    }
    @IBAction func btnSMS(_ sender: Any) {
        selectedOption = .sms
        refresh()
    }
    
    @IBAction func btnCall(_ sender: Any) {
        selectedOption = .call
        refresh()
    }

    @IBAction func btnCancel(_ sender: Any) {
        dismiss()
    }
    
    @IBAction func btnOk(_ sender: Any) {
        dismiss()
    }
}
