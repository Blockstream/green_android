import UIKit

class WOSelectViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    let viewModel = WOSelectViewModel()

    override func viewDidLoad() {
        super.viewDidLoad()

        ["WOTypeCell",].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        setContent()
        setStyle()
    }

    func setContent() {
        lblTitle.text = "id_select_watchonly_type".localized
        lblHint.text = "id_choose_the_security_policy_that".localized
    }

    func setStyle() {
        lblTitle.setStyle(.title)
        lblHint.setStyle(.txt)
    }
}

extension WOSelectViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel.types.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: WOTypeCell.identifier, for: indexPath) as? WOTypeCell {
            let model = viewModel.types[indexPath.row]
            cell.configure(model)
            cell.selectionStyle = .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let hwFlow = UIStoryboard(name: "WOFlow", bundle: nil)
        switch indexPath.row {
        case 0: // SS
            if let vc = hwFlow.instantiateViewController(withIdentifier: "WODetailsViewController") as? WODetailsViewController {
                navigationController?.pushViewController(vc, animated: true)
            }
        case 1: // MS
            if let vc = hwFlow.instantiateViewController(withIdentifier: "WOSetupViewController") as? WOSetupViewController {
                navigationController?.pushViewController(vc, animated: true)
            }
        default:
            break
        }
    }
}
