import Foundation
import UIKit

protocol DialogQRCodeScanViewControllerDelegate: AnyObject {
    func didScan(value: String, index: Int?)
    func didStop()
}

enum QRCodeScanAction {
    case scan(result: String)
    case stop
}

class DialogQRCodeScanViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var qrScanView: QRCodeReaderView!

    var index: Int?

    weak var delegate: DialogQRCodeScanViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = NSLocalizedString("id_scan_qr_code", comment: "")
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        qrScanView.delegate = self
        view.alpha = 0.0
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
            self.startCapture()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        qrScanView.stopScan()
    }

    private func startCapture() {
        if qrScanView.isSessionNotDetermined() {
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
                self.startCapture()
            }
            return
        }
        if !qrScanView.isSessionAuthorized() {
            return
        }
        qrScanView.startScan()
    }

    func dismiss(_ action: QRCodeScanAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .stop:
                self.delegate?.didStop()
            case .scan(let result):
                self.delegate?.didScan(value: result, index: self.index)
            }
        })
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.stop)
    }

}

extension DialogQRCodeScanViewController: QRCodeReaderDelegate {
    func userDidGrant(_: Bool) {
        //
    }

    func onQRCodeReadSuccess(result: String) {
        qrScanView.stopScan()
        dismiss(.scan(result: result))
//        createTransaction(userInput: result)
    }
}
