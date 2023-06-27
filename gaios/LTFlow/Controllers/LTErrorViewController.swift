import Foundation
import UIKit

protocol LTErrorViewControllerDelegate: AnyObject {
    func onReport(_ errorStr: String?)
    func onDone()
}

enum LTErrorAction {
    case report
    case done
}

class LTErrorViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint1: UILabel!
    @IBOutlet weak var lblHint2: UILabel!

    @IBOutlet weak var btnReport: UIButton!
    @IBOutlet weak var btnDone: UIButton!
    
    weak var delegate: LTErrorViewControllerDelegate?

    var errorStr: String?

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
        lblTitle.text = "id_error".localized
        lblHint1.text = "An unidentified error occured. The following error log can help you identify the issue:"
        lblHint2.text = errorStr
        btnReport.setTitle("Report", for: .normal)
        btnDone.setTitle("OK", for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.setStyle(.txtBigger)
        lblHint1.setStyle(.txtCard)
        lblHint2.setStyle(.txtCard)
        btnReport.setStyle(.primary)
        btnDone.setStyle(.primary)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss(_ action: LTErrorAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                switch action {
                case .report:
                    self.delegate?.onReport(self.errorStr)
                case .done:
                    self.delegate?.onDone()
                }
            })
        })
    }

    @IBAction func btnReport(_ sender: Any) {
        dismiss(.report)
    }

    @IBAction func btnDone(_ sender: Any) {
        dismiss(.done)
    }
    
}
