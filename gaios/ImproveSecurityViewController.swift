import Foundation
import UIKit

class ImproveSecurityViewController: UIViewController {

    @IBOutlet weak var skipButton: UIButton!
    @IBOutlet weak var twoFactorButton: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()
        skipButton.contentHorizontalAlignment = .left
        twoFactorButton.contentHorizontalAlignment = .left
        skipButton.setTitle(NSLocalizedString("id_skip_not_recommended", comment: ""), for: .normal)
        twoFactorButton.setTitle(NSLocalizedString("id_set_up_twofactor_authentication", comment: ""), for: .normal)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let pinController = segue.destination as? PinLoginViewController {
            pinController.setPinMode = true
        }
    }

    @IBAction func skipButtonClicked(_ sender: Any) {
        self.performSegue(withIdentifier: "mainMenu", sender: self)
    }

    @IBAction func backButtonClicked(_ sender: Any) {
        navigationController?.popViewController(animated: true)
    }
}
