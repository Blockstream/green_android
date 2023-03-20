import Foundation
import UIKit
import PromiseKit

enum UpdateFirmwareAction {
    case update
    case skip
}

protocol UpdateFirmwareViewControllerDelegate: AnyObject {
    func didSelectAction(_ action: UpdateFirmwareAction)
}

class UpdateFirmwareViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnUpdate: UIButton!
    @IBOutlet weak var btnSkip: UIButton!

    weak var delegate: UpdateFirmwareViewControllerDelegate?

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
        lblTitle.text = "Update Blockstream Jade to the latest version".localized
        lblHint.text = "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum".localized
        btnUpdate.setTitle("id_update".localized, for: .normal)
        btnSkip.setTitle("id_skip".localized, for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.font = UIFont.systemFont(ofSize: 24.0, weight: .bold)
        lblHint.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        lblTitle.textColor = .white
        lblHint.textColor = .white.withAlphaComponent(0.6)
        btnUpdate.setStyle(.primary)
        btnSkip.setStyle(.inline)
        btnSkip.setTitleColor(.white, for: .normal)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

    }

    func dismiss(_ action: UpdateFirmwareAction) {
        switch action {
        case .update:
            print("update")
        case .skip:
            print("skip")
        }
        delegate?.didSelectAction(action)
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
        })
    }

    @IBAction func btnUpdate(_ sender: Any) {
        dismiss(.update)
    }

    @IBAction func btnSkip(_ sender: Any) {
        dismiss(.skip)
    }
}
