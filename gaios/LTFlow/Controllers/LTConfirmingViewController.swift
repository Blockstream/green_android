import Foundation
import UIKit

class LTConfirmingViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var animateView: UIView!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        let riveView = RiveModel.animationLightningTransaction.createRiveView()
        riveView.frame = CGRect(x: 0.0, y: 0.0, width: animateView.frame.width, height: animateView.frame.height)
        animateView.addSubview(riveView)

        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func setContent() {
        lblTitle.text = "id_confirming_your_transaction".localized
        lblHint.text = "id_this_might_take_up_to_a".localized
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.setStyle(.txtBigger)
        lblHint.setStyle(.txt)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss() {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
            })
        })
    }

    @IBAction func btnContinue(_ sender: Any) {
        dismiss()
    }
}
