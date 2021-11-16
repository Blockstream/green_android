import UIKit

enum TransactionSection: Int, CaseIterable {
    case amount = 0
    case fee = 1
    case status = 2
    case detail = 3
}

class TransactionViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    override func viewDidLoad() {
        super.viewDidLoad()

        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
    }

    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        tableView.reloadData { [weak self] in
            self?.tableView.refreshControl?.endRefreshing()
        }
    }

    func editNote() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogNoteViewController") as? DialogNoteViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }
}

extension TransactionViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return TransactionSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case TransactionSection.amount.rawValue:
            return 2
        case TransactionSection.fee.rawValue:
            return 1
        case TransactionSection.status.rawValue:
            return 1
        case TransactionSection.detail.rawValue:
            return 1
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case TransactionSection.amount.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionAmountCell") as? TransactionAmountCell {
                cell.configure()
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.fee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionFeeCell") as? TransactionFeeCell {
                cell.configure()
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.status.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionStatusCell") as? TransactionStatusCell {
                cell.configure(num: 1)
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.detail.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionDetailCell") as? TransactionDetailCell {
                cell.configure(noteAction: { [weak self] in
                    self?.editNote()
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
        switch section {
        case TransactionSection.detail.rawValue:
            return headerH
        default:
            return 1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch section {
        case TransactionSection.detail.rawValue:
            return headerView("Transaction details")
        default:
            return headerView("")
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

    }
}

extension TransactionViewController {
    func headerView(_ txt: String) -> UIView {
        if txt == "" {
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        }
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.customTitaniumDark()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 20.0, weight: .heavy)
        title.text = txt
        title.textColor = .white
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -20)
        ])

        return section
    }
}

extension TransactionViewController: DialogNoteViewControllerDelegate {

    func didSave(_ note: String) {
        print(note)
    }

    func didCancel() {
        print("cancel")
    }
}
