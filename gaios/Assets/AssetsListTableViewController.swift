import Foundation
import UIKit
import PromiseKit

class AssetsListTableViewController: UITableViewController {

    var wallet: WalletItem?
    var transaction: Transaction!
    var isSend: Bool = false

    private var assets: [(key: String, value: Balance)] {
        get {
            var list = wallet!.balance
            let btc = list.removeValue(forKey: "btc")
            var sorted = list.sorted(by: {$0.0 < $1.0 })
            sorted.insert((key: "btc", value: btc!), at: 0)
            return Array(sorted)
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        let nib = UINib(nibName: "AssetTableCell", bundle: nil)
        tableView.register(nib, forCellReuseIdentifier: "cell")
        tableView.tableFooterView = UIView()
        tableView.estimatedRowHeight = 70
        tableView.rowHeight = UITableView.automaticDimension
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if !wallet!.balance.isEmpty {
            return self.tableView.reloadData()
        }
        // reload if empty balance
        wallet!.getBalance().done { _ in
            self.tableView.reloadData()
        }.catch { _ in }
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
        let asset = assets[indexPath.row].value.assetInfo
        let satoshi = assets[indexPath.row].value.satoshi
        cell.configure(tag: tag, asset: asset, satoshi: satoshi)
        cell.selectionStyle = .none
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let tag = assets[indexPath.row].key
        if !isSend && tag == "btc" { return }
        if isSend {
            performSegue(withIdentifier: "asset_send", sender: tag)
        } else {
            performSegue(withIdentifier: "asset", sender: tag)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let next = segue.destination as? AssetDetailTableViewController {
            next.tag = sender as? String
            next.asset = wallet?.balance[next.tag]?.assetInfo
            next.satoshi = wallet?.balance[next.tag]?.satoshi
        } else if let next = segue.destination as? SendBtcDetailsViewController {
            next.wallet = wallet
            next.assetTag = sender as? String
            next.transaction = transaction
        }
    }
}
