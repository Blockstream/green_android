import Foundation
import UIKit
import gdk

protocol DenominationExchangeViewControllerDelegate: AnyObject {
    func onDenominationExchangeSave()
}

enum DenExAction {
    case ok
    case cancel
}

class DenominationExchangeViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!

    @IBOutlet weak var denBg: UIView!
    @IBOutlet weak var lblDenTitle: UILabel!
    @IBOutlet weak var lblDenHint: UILabel!
    @IBOutlet weak var exBg: UIView!
    @IBOutlet weak var lblExTitle: UILabel!
    @IBOutlet weak var lblExHint: UILabel!

    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnOk: UIButton!

    let viewModel = DenominationExchangeViewModel()
    weak var delegate: DenominationExchangeViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
        refresh()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func setContent() {
        lblTitle.text = "Denomination and Exchange Rate"
        btnCancel.setTitle("id_cancel".localized, for: .normal)
        btnOk.setTitle("OK", for: .normal)
        lblDenTitle.text = "id_denomination".localized
        lblExTitle.text = "id_reference_exchange_rate".localized
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        lblTitle.setStyle(.titleCard)
        btnCancel.setStyle(.inline)
        btnOk.setStyle(.inline)
        [lblDenTitle, lblExTitle].forEach{
            $0?.setStyle(.txtSmaller)
            $0?.textColor = .gW40()
        }
        [lblDenHint, lblExHint].forEach{
            $0?.setStyle(.txtSmallerBold)
        }
        [denBg, exBg].forEach{
            $0?.cornerRadius = 5.0
        }
    }

    func refresh() {
        lblDenHint.text = viewModel.currentSymbolStr()
        lblExHint.text = viewModel.currentExchange()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    @MainActor
    func dismiss(_ action: DenExAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                switch action {
                case .ok:
                    self.delegate?.onDenominationExchangeSave()
                case .cancel:
                    break
                }
            })
        })
    }

    func showDialogDenominations() {
        guard let model = viewModel.dialogDenominationViewModel() else { return }

        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogDenominationViewController") as? DialogDenominationViewController {
            vc.viewModel = model
            vc.delegate = self
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func showDialogExchanges() {
        let storyboard = UIStoryboard(name: "DenominationExchangeFlow", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogExchangeViewController") as? DialogExchangeViewController {
            vc.delegate = self
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnCancel(_ sender: Any) {
        dismiss(.cancel)
    }

    @IBAction func btnOK(_ sender: Any) {
        guard let settings = viewModel.session?.settings else { return }

        if let denomination = viewModel.editingDenomination {
            settings.denomination = denomination
        }
        if let pricing = viewModel.pricing() {
            settings.pricing = pricing
        }
        Task {
            do {
                self.startAnimating()
                _ = try await self.viewModel.session?.changeSettings(settings: self.viewModel.settings!)
                self.dismiss(.ok)
            } catch {
                self.showError(error)
            }
            self.stopAnimating()
        }
    }

    @IBAction func btnDen(_ sender: Any) {
        showDialogDenominations()
    }

    @IBAction func btnEx(_ sender: Any) {
        showDialogExchanges()
    }
}

extension DenominationExchangeViewController: DialogDenominationViewControllerDelagate {
    func didSelect(denomination: DenominationType) {
        viewModel.editingDenomination = denomination
        refresh()
    }
}

extension DenominationExchangeViewController: DialogExchangeViewControllerDelagate {
    func didSelect(currencyItem: CurrencyItem) {
        viewModel.editingExchange = currencyItem
        refresh()
    }
    
    
}
