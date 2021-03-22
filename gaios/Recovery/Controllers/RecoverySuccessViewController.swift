import UIKit

class RecoverySuccessViewController: UIViewController {

    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var successCircle: UIView!
    @IBOutlet weak var btnNext: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()
    }

    func setContent() {
        title = ""
        lblHint.text = "Success"
        btnNext.setTitle("Continue", for: .normal)
    }

    func setStyle() {
        btnNext.cornerRadius = 4.0
        successCircle.borderWidth = 2.0
        successCircle.layer.cornerRadius = successCircle.frame.size.width / 2.0
        successCircle.borderColor = UIColor.customMatrixGreen()
    }

    func setActions() {

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }

    @IBAction func btnNext(_ sender: Any) {
        navigationController?.popToRootViewController(animated: true)
    }

}
