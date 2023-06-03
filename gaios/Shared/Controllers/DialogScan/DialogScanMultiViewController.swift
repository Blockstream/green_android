import Foundation
import BreezSDK
import PromiseKit
import lightning
import UIKit
import gdk
/*
protocol DialogScanMultiViewControllerDelegate: AnyObject {
    func didScanBolt11(_ value: LnInvoice)
    func didScanLnUrlAuth(_ value: LnUrlAuthRequestData)
    func didScanAddress(_ value: String)
    func didScanSwap(_ value: String)
    func didScanError(_ value: String)
    func didStop()
}

class DialogScanMultiViewController: DialogScanViewControllerDelegate {

    var account: WalletItem
    var delegate: DialogScanMultiViewControllerDelegate
    private let bgq = DispatchQueue.global(qos: .background)

    init(account: WalletItem, delegate: DialogScanMultiViewControllerDelegate) {
        self.account = account
        self.delegate = delegate
    }

    private static var IgnorableErrors = [
    "id_invalid_amount",
    "id_no_amount_specified",
    "id_insufficient_funds",
    "id_invalid_payment_request_assetid",
    "id_invalid_asset_id" ]

    var vc: UIViewController? {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogScanViewController") as? DialogScanViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            return vc
        }
        return nil
    }
        
    func didScan(value: String, index: Int?) {
        parseInput(input: value, selectedAccount: account)
    }

    func didStop() {
        delegate.didStop()
    }

    func parseInput(input: String, selectedAccount: WalletItem?) {
        // lightning
        if let session = WalletManager.current?.lightningSession {
            switch session.parseTxInput(input) {
            case .bitcoinAddress(let address):
                self.delegate.didScanAddress(address.address)
            case .bolt11(let invoice):
                delegate.didScanBolt11(invoice)
                return
            case .lnUrlAuth(let data):
                delegate.didScanLnUrlAuth(data)
                return
            case .lnUrlWithdraw(_):
                delegate.didScanError("Not supported".localized)
                return
            default:
                break
            }
        }
        // swaps
        if input.starts(with: "https://") {
            delegate.didScanSwap(input)
            return
        }
        // bitcoin / liquid
        guard let account = selectedAccount,
              let session = WalletManager.current?.sessions[account.networkType.rawValue] else { return }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
            .then(on: bgq) { session.parseTxInput(input, satoshi: nil, assetId: nil) }
            .done {
                if $0.isValid {
                    self.delegate.didScanAddress(input)
                } else if let error = $0.errors.first {
                    if DialogScanMultiViewController.IgnorableErrors.contains(error) {
                        self.delegate.didScanAddress(input)
                    } else {
                        self.delegate.didScanError(error)
                    }
                }
            }
            .catch { _ in self.delegate.didScanError("Invalid qrcode".localized) }
    }
}*/
