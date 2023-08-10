import Foundation
import UIKit
import BreezSDK
import greenaddress
import lightning

class LTAuthViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblInfo: UILabel!
    @IBOutlet weak var btnAuth: UIButton!

    var requestData: LnUrlAuthRequestData?

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
    }

    func setContent() {
        title = "LNURL Auth"
        lblTitle.text = "id_you_can_use_your_wallet_to".localized
        lblInfo.text = "id_no_personal_data_will_be_shared".localized
        btnAuth.setTitle("id_authenticate".localized, for: .normal)
        reloadData()
    }

    func setStyle() {
        lblTitle.setStyle(.txtBigger)
        lblHint.setStyle(.subTitle)
        lblInfo.setStyle(.txt)
        btnAuth.setStyle(.primary)
    }

    func reloadData() {
        lblHint.text = requestData?.domain
    }

    @IBAction func btnAuth(_ sender: Any) {
        if let requestData = requestData {
            lnAuth(requestData: requestData)
        }
    }

    func lnAuth(requestData: LnUrlAuthRequestData) {
        startAnimating()
        Task {
            do {
                let lightBridge = WalletManager.current?.lightningSession?.lightBridge
                guard let res = try lightBridge?.authLnUrl(requestData: requestData) else {
                    throw GaError.GenericError()
                }
                switch res {
                case .ok:
                    DropAlert().success(message: "Authentication successful")
                    self.navigationController?.popViewController(animated: true)
                case .errorStatus(let data):
                    DropAlert().error(message: data.reason)
                }
            } catch {
                switch error {
                case BreezSDK.SdkError.Generic(let msg),
                    BreezSDK.SdkError.LspConnectFailed(let msg),
                    BreezSDK.SdkError.PersistenceFailure(let msg),
                    BreezSDK.SdkError.ReceivePaymentFailed(let msg):
                    DropAlert().error(message: msg.localized)
                default:
                    DropAlert().error(message: "id_operation_failure".localized)
                }
            }
            stopAnimating()
        }
    }
}
