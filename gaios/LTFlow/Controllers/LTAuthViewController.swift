import Foundation
import UIKit
import BreezSDK
import PromiseKit
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
        lblTitle.text = "You can use your wallet to anonymously sign and authorize an action on:"
        lblInfo.text = "No personal data will be shared with this service."
        btnAuth.setTitle("Authenticate", for: .normal)
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
        guard let lightBridge = WalletManager.current?.lightningSession?.lightBridge else { return }
        let bgq = DispatchQueue.global(qos: .background)
        startAnimating()
        Guarantee()
            .compactMap(on: bgq) { lightBridge.authLnUrl(requestData: requestData) }
            .ensure { [self] in stopAnimating() }
            .done {
                switch $0 {
                case LnUrlCallbackStatus.ok:
                    DropAlert().success(message: "Authentication successful")
                    self.navigationController?.popViewController(animated: true)
                case LnUrlCallbackStatus.errorStatus(let data):
                    DropAlert().error(message: data.reason)
                }
            }
            .catch {
                switch $0 {
                case BreezSDK.SdkError.Generic(let msg),
                    BreezSDK.SdkError.LspConnectFailed(let msg),
                    BreezSDK.SdkError.PersistenceFailure(let msg),
                    BreezSDK.SdkError.ReceivePaymentFailed(let msg):
                    DropAlert().error(message: msg.localized)
                default:
                    DropAlert().error(message: "id_operation_failure".localized)
                }
            }
    }
}
