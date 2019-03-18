import Foundation
import UIKit

class InstructionViewController: UIViewController {

    @IBOutlet var content: InstructionView!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_save_your_mnemonic", comment: "")
        content.topLabel.text = NSLocalizedString("id_write_down_your_mnemonic_on", comment: "")
        content.middleLabel.text = NSLocalizedString("id_dont_store_your_mnemonic_on", comment: "")
        content.bottomLabel.text = NSLocalizedString("id_dont_take_screenshots_of_your", comment: "")
        content.nextButton.setTitle(NSLocalizedString("id_next", comment: ""), for: .normal)
        content.nextButton.setGradient(true)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.nextButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        content.nextButton.updateGradientLayerFrame()
    }

    @objc func click(_ sender: UIButton) {
        self.performSegue(withIdentifier: "next", sender: self)
    }
}

@IBDesignable
class InstructionView: UIView {
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var bottomLabel: UILabel!
    @IBOutlet weak var middleLabel: UILabel!
    @IBOutlet weak var topLabel: UILabel!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        nextButton.updateGradientLayerFrame()
    }
}
