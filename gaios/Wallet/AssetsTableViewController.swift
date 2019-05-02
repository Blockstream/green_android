import Foundation
import UIKit
import PromiseKit

protocol AssetsDelegate: class {
    func onSelect(_ tag: String)
}

class AssetsTableViewController: UITableViewController {

    var wallet: WalletItem?
    weak var delegate: AssetsDelegate?
    private var assets: [(key: String, value: Balance)] {
        get {
            var list = wallet!.balance
            let btc = list.removeValue(forKey: "btc")
            var sorted = list.sorted(by: {$0.0 < $1.0 })
            sorted.insert((key: "L-BTC", value: btc!), at: 0)
            return Array(sorted)
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        let nib = UINib(nibName: "AssetTableCell", bundle: nil)
        tableView.register(nib, forCellReuseIdentifier: "cell")
        tableView.tableFooterView = UIView()

        wallet!.getBalance().done { _ in
            self.tableView.reloadData()
        }.catch { _ in }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return assets.count
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 70
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as? AssetTableCell else { fatalError("Fail to dequeue reusable cell") }
        let tag = assets[indexPath.row].key
        let balance = assets[indexPath.row].value
        cell.title.text = tag
        cell.value.text = balance.btc
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let tag = assets[indexPath.row].key
        delegate?.onSelect(tag)
        navigationController?.popViewController(animated: true)
    }

}
