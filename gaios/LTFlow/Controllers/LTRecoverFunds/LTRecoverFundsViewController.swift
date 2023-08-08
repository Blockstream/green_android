import Foundation
import UIKit
import BreezSDK
import gdk
import greenaddress

enum LTRecoverFundsType {
    case sweep
    case refund
}

struct LTRecoverFundsViewModel {
    var wallet: WalletItem?
    var address: String?
    var onChainAddress: String?
    var amount: UInt64?
    var type: LTRecoverFundsType
    var feeSlider: Int = 0
    var session: LightningSessionManager? { wallet?.lightningSession }
    var recommendedFees: RecommendedFees?
    var fee: UInt64? {
        if let fees = recommendedFees {
            let rules = [fees.economyFee, fees.hourFee, fees.halfHourFee, fees.fastestFee]
            return rules[feeSlider]
        }
        return recommendedFees?.economyFee
    }
    var feeAmountRate: String? { feeRateWithUnit((fee ?? 0) * 1000) }
    func feeRateWithUnit(_ value: UInt64) -> String {
        let feePerByte = Double(value) / 1000.0
        return String(format: "%.2f sats / vbyte", feePerByte)
    }
    
    func sweep() async throws {
        if let address = address {
            let fee = fee.map {UInt($0)}
            try await session?.lightBridge?.sweep(toAddress: address, satPerVbyte: fee)
        } else {
            throw GaError.GenericError("Invalid Address")
        }
    }

    func refund() async throws {
        if let swapAddress = onChainAddress, let address = address {
            let fee = fee.map {UInt32($0)}
            try await session?.lightBridge?.refund(swapAddress: swapAddress, toAddress: address, satPerVbyte: fee)
        } else {
            throw GaError.GenericError("Invalid Address")
        }
    }
    
    func recoverFunds() async throws {
        switch type {
        case .refund:
            try await refund()
        case .sweep:
            try await sweep()
        }
    }
}

class LTRecoverFundsViewController: KeyboardViewController {

    enum LTRecoverFundsSection: Int, CaseIterable {
        case address
        case amount
        case fee
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!
    private var headerH: CGFloat = 36.0

    var viewModel: LTRecoverFundsViewModel!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        reload()
    }

    func setContent() {
        title = viewModel.type == .refund ? "Refund" : "Close channel"
        btnNext.setTitle("Confirm", for: .normal)
    }

    func setStyle() {
        btnNextIsEnabled = false
    }

    var btnNextIsEnabled: Bool {
        get { btnNext.isEnabled }
        set {
            btnNext.setStyle(newValue ? .primary : .primaryGray)
            btnNext.isEnabled = newValue
        }
    }

    override func keyboardWillHide(notification: Notification) {
        if keyboardDismissGesture != nil {
            view.removeGestureRecognizer(keyboardDismissGesture!)
            keyboardDismissGesture = nil
        }
        tableView.setContentOffset(CGPoint(x: 0.0, y: 0.0), animated: true)
    }
    
    func reload() {
        Task.detached() { [weak self] in
            let fees = try? await self?.viewModel?.session?.lightBridge?.recommendedFees()
            await MainActor.run { [weak self] in
                self?.viewModel?.recommendedFees = fees
                self?.tableView.reloadData()
            }
        }
    }
    
    @IBAction func btnNext(_ sender: Any) {
        Task {
            startAnimating()
            do {
                try await viewModel.recoverFunds()
                stopAnimating()
                success()
            } catch {
                stopAnimating()
                showError(error)
            }
        }
    }
    
    @MainActor
    func success() {
        if viewModel.type == .refund {
            showAlert(title: "Refund", message: "We have initiated your refund") {
                self.navigationController?.popToRootViewController(animated: true)
            }
        } else {
            showAlert(title: "Close channel", message: "We have initiated transfer of your funds") {
                self.navigationController?.popToRootViewController(animated: true)
            }
        }
    }
}

extension LTRecoverFundsViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return LTRecoverFundsSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch LTRecoverFundsSection(rawValue: section) {
        case .address:
            return 1
        case .amount:
            return 1
        case .fee:
            return 1
        case .none:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        switch LTRecoverFundsSection(rawValue: indexPath.section) {

        case .address:
            if let cell = tableView.dequeueReusableCell(withIdentifier: LTRecoverFundsAddressCell.identifier) as? LTRecoverFundsAddressCell {
                cell.configure(address: viewModel.address ?? "")
                cell.delegate = self
                cell.selectionStyle = .none
                return cell
            }
        case .amount:
            if let cell = tableView.dequeueReusableCell(withIdentifier: LTRecoverFundsAmountCell.identifier) as? LTRecoverFundsAmountCell {
                cell.configure(amount: viewModel.amount ?? 0)
                cell.selectionStyle = .none
                return cell
            }
        case .fee:
            if let cell = tableView.dequeueReusableCell(withIdentifier: LTRecoverFundsFeeCell.identifier) as? LTRecoverFundsFeeCell {
                cell.configure(feeRate: viewModel.feeAmountRate ?? "", feeSliderIndex: viewModel.feeSlider, feeSliderMaxIndex: 3)
                cell.delegate = self
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch LTRecoverFundsSection(rawValue: section) {
        case .address:
            return UITableView.automaticDimension
        case .amount:
            return UITableView.automaticDimension
        case .fee:
            return UITableView.automaticDimension
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch LTRecoverFundsSection(rawValue: section) {
        case .address:
            return headerView("Receive".localized)
        case .amount:
            return headerView("id_amount".localized)
        case .fee:
            return headerView("Fee Network".localized)
        case .none:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    }

    func headerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.gBlackBg()
        let title = UILabel(frame: .zero)
        title.setStyle(.sectionTitle)
        title.text = txt
        title.numberOfLines = 0
        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)
        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 25),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -25)
        ])
        return section
    }
}

extension LTRecoverFundsViewController: LTRecoverFundsAddressCellDelegate {
    func didChange(address: String) {
        viewModel.address = address
        btnNextIsEnabled = !address.isEmpty
        tableView.reloadData()
    }
    
    func qrcodeScanner() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogScanViewController") as? DialogScanViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.index = nil
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }
}

extension LTRecoverFundsViewController: DialogScanViewControllerDelegate {
    func didScan(value: String, index: Int?) {
        viewModel.address = value
        btnNextIsEnabled = !value.isEmpty
        tableView.reloadData()
    }
    func didStop() {
    }
}

extension LTRecoverFundsViewController: LTRecoverFundsFeeDelegate {
    func didChange(feeSliderIndex: Int) {
        viewModel.feeSlider = feeSliderIndex
        tableView.reloadData()
    }
}
