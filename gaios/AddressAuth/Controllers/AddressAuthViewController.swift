import UIKit

import RiveRuntime
import gdk
import BreezSDK

enum AddressAuthSection: Int, CaseIterable {
    case list
}

class AddressAuthViewController: KeyboardViewController {

    @IBOutlet weak var searchCard: UIView!
    @IBOutlet weak var searchField: UITextField!
    @IBOutlet weak var tableView: UITableView!
    
    var viewModel: AddressAuthViewModel?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        ["AddressAuthCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        searchField.delegate = self
        setContent()
        setStyle()
        navigationItem.rightBarButtonItems = []
        loadNavigationBtns()
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
    
    
    @MainActor
    func reloadSections(_ sections: [AddressAuthSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
            self.tableView.refreshControl?.endRefreshing()
        }
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
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
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
        reload()
    }
    
    func reload() {
        Task {
            //            if isReloading { return }
            //            isReloading = true
            //            await viewModel.loadSubaccounts()
            reloadSections([.list], animated: true)
            //            try? await viewModel.loadBalances()
            //            reloadSections([.account, .balance, .card], animated: true)
            //            await viewModel.reloadAlertCards()
            //            reloadSections([.card], animated: true)
            //            try? await viewModel.loadTransactions(max: 10)
            //            reloadSections([.transaction], animated: true)
            //            isReloading = false
        }
    }

    func onCopy(_ row: Int) {
        print( viewModel?.listCellModelsFilter[row].tx ?? "Err")
    }

    func onSign(_ row: Int) {
        guard let model = viewModel?.listCellModelsFilter[safe: row] else { return }
        let storyboard = UIStoryboard(name: "AddressAuth", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogSignViewController") as? DialogSignViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.address = model.address
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
            return 0
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
}

extension AddressAuthViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}
