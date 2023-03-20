import Foundation
import UIKit
import PromiseKit

protocol UpdateFirmwareViewControllerDelegate: AnyObject {
    func didUpdate(_ firmware: Firmware)
    func didSkip()
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
    var version: String!
    var firmware: Firmware!

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
        lblHint.text = "Current firmware: \(version ?? "")\nLatest firmware: \(firmware.version)"
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

    func dismiss() {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
        })
    }

    @IBAction func btnUpdate(_ sender: Any) {
        delegate?.didUpdate(firmware)
        dismiss()
    }

    @IBAction func btnSkip(_ sender: Any) {
        delegate?.didSkip()
        dismiss()
    }
}
