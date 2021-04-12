import Foundation
import UIKit
import PromiseKit

class NotificationsViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {

    @IBOutlet weak var tableView: UITableView!
    private var items: [Event] { get { return getGAService().getEvents() } }
    private var twoFactorConfig: TwoFactorConfig?
    private var wallets = [WalletItem]()

    private var transactionToken: NSObjectProtocol?
    private var twoFactorResetToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        self.navigationItem.title = NSLocalizedString("id_notifications", comment: "")
        tableView.delegate = self
        tableView.dataSource = self
        tableView.tableFooterView = UIView()

        let closeButton = UIBarButtonItem(image: UIImage(named: "cancel"),
                                     style: .plain,
                                     target: self,
                                     action: #selector(close))
        closeButton.tintColor = UIColor.customGrayLight()
        self.navigationItem.rightBarButtonItem = closeButton
    }

    @objc func close() {
        dismiss(animated: true, completion: nil)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        transactionToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.TwoFactorReset.rawValue), object: nil, queue: .main, using: self.reloadData)
        twoFactorResetToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil, queue: .main, using: self.reloadData)
        reloadData(nil)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = transactionToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = twoFactorResetToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    func reloadData(_ notification: Notification?) {
        let bgq = DispatchQueue.global(qos: .background)
        getSubaccounts().map(on: bgq) { wallets in
            self.wallets = wallets
            let dataTwoFactorConfig = try getSession().getTwoFactorConfig()
            self.twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: []))
        }.done {
            self.tableView.reloadData()
        }.catch {_ in }
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {let cell =
        tableView.dequeueReusableCell(withIdentifier: "cell",
                                      for: indexPath as IndexPath)
        let event = items[indexPath.row]
        cell.textLabel!.text = event.title()
        cell.detailTextLabel!.text = event.description(wallets: wallets, twoFactorConfig: twoFactorConfig)
        cell.detailTextLabel!.numberOfLines = 4
        cell.selectionStyle = .none
        cell.setNeedsLayout()
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let event = items[indexPath.row]
        if event.kindOf(Settings.self) {
            self.performSegue(withIdentifier: "twofactor", sender: event)
        } else if event.kindOf(SystemMessage.self) {
            self.performSegue(withIdentifier: "system_message", sender: event)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? EnableTwoFactorViewController {
            nextController.isHiddenWalletButton = true
        } else if let nextController = segue.destination as? SystemMessageViewController {
            nextController.systemMessage = sender as? Event
        }
    }
}
