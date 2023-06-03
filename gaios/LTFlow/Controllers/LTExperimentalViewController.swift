import Foundation
import UIKit

protocol LTExperimentalViewControllerDelegate: AnyObject {
    func onDone()
}

class LTExperimentalViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnContinue: UIButton!

    weak var delegate: LTExperimentalViewControllerDelegate?

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
        lblTitle.text = "Experimental Feature"
        lblHint.text = "Experimental features might change, break, or be discontinued at any time, so you agree to use them at your own risk."
        btnContinue.setTitle("Ok, I understand.", for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.setStyle(.txtBigger)
        lblHint.setStyle(.txt)
        btnContinue.setStyle(.primary)
    }

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

    @IBAction func btnContinue(_ sender: Any) {
        dismiss()
    }
}
