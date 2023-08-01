import UIKit

import RiveRuntime
import gdk
import BreezSDK

enum AddressAuthSection: Int, CaseIterable {
    case list
    case loader
}

class AddressAuthViewController: KeyboardViewController {

    @IBOutlet weak var searchCard: UIView!
    @IBOutlet weak var searchField: UITextField!
    @IBOutlet weak var tableView: UITableView!
    
    var viewModel: AddressAuthViewModel!
    var isSearchActive = false

    override func viewDidLoad() {
        super.viewDidLoad()
        
        ["AddressAuthCell", "AddressAuthLoaderCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        searchField.delegate = self
        setContent()
        setStyle()
        navigationItem.rightBarButtonItems = []
        loadNavigationBtns()
        fetchData(reset: true)
    }
    
    deinit {
        print("deinit")
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
    }
    
    func setContent() {
        title = "id_search_address".localized
        
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(callPullToRefresh(_:)), for: .valueChanged)
        searchField.attributedPlaceholder = NSAttributedString(string: "Search", attributes: [NSAttributedString.Key.foregroundColor: UIColor.white.withAlphaComponent(0.4)])
    }
    
    func loadNavigationBtns() {
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("Export".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(exportBtnTapped), for: .touchUpInside)
        //navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }
    
    func setStyle() {
        searchCard.cornerRadius = 5.0
    }

    @objc func triggerTextChange() {
        viewModel?.search(searchField.text ?? "")
        tableView.reloadData()
    }

    @objc func exportBtnTapped() {
        // SafeNavigationManager.shared.navigate( ExternalUrls.jadeTroubleshoot )
    }
    
    // tableview refresh gesture
    @objc func callPullToRefresh(_ sender: UIRefreshControl? = nil) {
        searchField.text = ""
        viewModel?.search(searchField.text ?? "")
        fetchData(reset: true)
    }
    
    func fetchData(reset: Bool) {
        Task {
            do {
                if viewModel?.listCellModelsFilter.count == 0 { startAnimating() }
                try await viewModel.fetchData(reset: reset)
                await MainActor.run {
                    stopAnimating()
                    tableView.reloadData { [weak self] in
                        self?.tableView.refreshControl?.endRefreshing()
                    }
                }
            } catch {
                stopAnimating()
                showError(error)
            }
        }
    }

    func onCopy(_ row: Int) {
        if let addr = viewModel?.listCellModelsFilter[safe: row]?.address {
            UIPasteboard.general.string = addr
            DropAlert().info(message: "id_copied_to_clipboard".localized)
        }
    }

    func onSign(_ row: Int) {
        guard let model = viewModel?.listCellModelsFilter[safe: row] else { return }
        let storyboard = UIStoryboard(name: "AddressAuth", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogSignViewController") as? DialogSignViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.viewModel = DialogSignViewModel(wallet: viewModel.wallet, address: model.address)
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func onEditingChange(_ sender: Any) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }
    
}

extension AddressAuthViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return AddressAuthSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch AddressAuthSection(rawValue: section) {
        case .list:
            return viewModel?.listCellModelsFilter.count ?? 0
        default:
            if isSearchActive { return 0 }
            return viewModel.isReady() ? 0 : 1
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch AddressAuthSection(rawValue: indexPath.section) {
        case .list:
            if let cell = tableView.dequeueReusableCell(withIdentifier: AddressAuthCell.identifier, for: indexPath) as? AddressAuthCell, let model = viewModel?.listCellModelsFilter[safe: indexPath.row] {
                cell.configure(
                    model: model,
                    onCopy: { [weak self] in
                        self?.onCopy(indexPath.row)
                    },
                    onSign: { [weak self] in
                        self?.onSign(indexPath.row)
                    })
                cell.selectionStyle = .none
                return cell
            }
        case .loader:
            if let cell = tableView.dequeueReusableCell(withIdentifier: AddressAuthLoaderCell.identifier, for: indexPath) as? AddressAuthLoaderCell {
                cell.configure()
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch AddressAuthSection(rawValue: section) {
        case .list:
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

        switch AddressAuthSection(rawValue: section) {
        case .list:
            if let headerView = Bundle.main.loadNibNamed("AddressAuthHeader", owner: self, options: nil)?.first as? AddressAuthHeader {
                headerView.configure()
                return headerView
            }
            return nil
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        switch AddressAuthSection(rawValue: indexPath.section) {
        case .list:
            return nil
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {}

    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        guard let section = AddressAuthSection(rawValue: indexPath.section) else { return }
        if section == .loader, isSearchActive == false {
            fetchData(reset: false)
        }
    }
}

extension AddressAuthViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
    
    func textFieldDidBeginEditing(_ textField: UITextField) {
        isSearchActive = true
    }
    
    func textFieldDidEndEditing(_ textField: UITextField) {
        isSearchActive = false
    }
}
