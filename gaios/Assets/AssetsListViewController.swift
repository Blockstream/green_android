import Foundation
import UIKit
import PromiseKit

protocol AssetsListViewControllerDelegate: AnyObject {
    func didSelect(assetId: String, index: Int?)
}

class AssetsListViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnDismiss: UIButton!

    var wallet: WalletItem?
    var index: Int?

    weak var delegate: AssetsListViewControllerDelegate?

    private var assets: [(key: String, value: Int64)] {
        get {
            return Transaction.sort(wallet!.satoshi ?? [:])
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_select_asset", comment: "")

        view.accessibilityIdentifier = AccessibilityIdentifiers.AssetsListScreen.view
        tableView.accessibilityIdentifier = AccessibilityIdentifiers.AssetsListScreen.table
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(onAssetsUpdated), name: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(animated: true)
    }

    @objc func onAssetsUpdated(_ notification: NSNotification) {
        self.tableView.reloadData()
    }
}

extension AssetsListViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        return assets.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "AssetCell") as? AssetCell {
            let tag = assets[indexPath.row].key
            let info = WalletManager.current?.currentSession?.registry?.info(for: tag)
            let icon = WalletManager.current?.currentSession?.registry?.image(for: tag)
            let satoshi = assets[indexPath.row].value
            cell.configure(tag: tag, info: info, icon: icon, satoshi: satoshi)
            cell.selectionStyle = .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let tag = assets[indexPath.row].key
        delegate?.didSelect(assetId: tag, index: index)
        dismiss(animated: true)
    }
}
