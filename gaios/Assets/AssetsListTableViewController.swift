import Foundation
import UIKit
import PromiseKit

protocol AssetsListTableViewControllerDelegate: AnyObject {
    func didSelect(assetId: String, index: Int?)
}

class AssetsListTableViewController: UITableViewController {

    var wallet: WalletItem?
    var transaction: Transaction!
    var isSend: Bool = false
    var index: Int?

    weak var delegate: AssetsListTableViewControllerDelegate?

    private var assets: [(key: String, value: UInt64)] {
        get {
            return Transaction.sort(wallet!.satoshi ?? [:])
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_select_asset", comment: "")
        let nib = UINib(nibName: "AssetTableCell", bundle: nil)
        tableView.register(nib, forCellReuseIdentifier: "cell")
        tableView.tableFooterView = UIView()
        tableView.estimatedRowHeight = 70
        tableView.rowHeight = UITableView.automaticDimension
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(onAssetsUpdated), name: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil)
    }

    @objc func onAssetsUpdated(_ notification: NSNotification) {
        Guarantee()
            .compactMap { Registry.shared.cache(session: SessionsManager.current) }
            .done { self.tableView.reloadData() }
            .catch { err in
                print(err.localizedDescription)
        }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return assets.count
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as? AssetTableCell else { fatalError("Fail to dequeue reusable cell") }
        let tag = assets[indexPath.row].key
        let info = Registry.shared.infos[tag]
        let icon = Registry.shared.image(for: tag)
        let satoshi = assets[indexPath.row].value
        cell.configure(tag: tag, info: info, icon: icon, satoshi: satoshi)
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let tag = assets[indexPath.row].key
        if isSend {
//            performSegue(withIdentifier: "asset_send", sender: tag)
            delegate?.didSelect(assetId: tag, index: index)
            dismiss(animated: true)
        } else {
            performSegue(withIdentifier: "asset", sender: tag)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let next = segue.destination as? AssetDetailTableViewController {
            next.tag = sender as? String
            next.asset = Registry.shared.infos[next.tag]
            next.satoshi = wallet?.satoshi?[next.tag]
        } else if let next = segue.destination as? SendBtcDetailsViewController {
            next.wallet = wallet
            next.assetId = sender as? String ?? "btc"
            next.transaction = transaction
        }
    }
}
