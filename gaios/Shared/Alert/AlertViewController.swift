import Foundation
import UIKit

protocol AlertViewControllerDelegate: AnyObject {
    func onAlertOk()
}

class AlertViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnOK: UIButton!

    var viewModel: AlertViewModel!
    weak var delegate: AlertViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func setContent() {
        lblTitle.text = viewModel.title
        lblHint.text = viewModel.hint
        btnOK.setTitle("id_ok".localized, for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.setStyle(.title)
        lblHint.setStyle(.txt)
        btnOK.setStyle(.primary)
        cardView.borderWidth = 1.0
        cardView.borderColor = .white.withAlphaComponent(0.05)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss() {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                self.delegate?.onAlertOk()
            })
        })
    }

    @IBAction func btnOk(_ sender: Any) {
        dismiss()
    }
}
