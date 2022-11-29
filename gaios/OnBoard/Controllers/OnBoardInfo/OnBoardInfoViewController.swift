import UIKit
import PromiseKit

enum OnBoardInfoSection: Int, CaseIterable {
    case info
    case footer
}

enum OnBoardInfoFlowType: Int, CaseIterable {
    case onboarding
    case subaccount
}

class OnBoardInfoViewController: UIViewController {

    enum FooterType {
        case none
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!
    @IBOutlet weak var btnPrint: UIButton!

    private var headerH: CGFloat = 54.0
    private var footerH: CGFloat = 54.0

    var viewModel = OnBoardInfoViewModel()
    static var flowType = OnBoardInfoFlowType.onboarding

    override func viewDidLoad() {
        super.viewDidLoad()

        ["OnBoardInfoCell" ].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }

        setContent()
        setStyle()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

    }

    func reloadSections(_ sections: [SecuritySelectSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
    }

    func setContent() {
        title = "Before You Backup"
        btnNext.setTitle("Show Recovery Phrase", for: .normal)
        btnPrint.setTitle("Print Backup Template", for: .normal)
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    func selectLength() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: NSLocalizedString("id_new_recovery_phrase", comment: ""), items: PhrasePrefs.getItems(), sender: 0)
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }

    }

    func next(_ lenght: MnemonicLengthOption) {
        OnBoardManager.shared.params?.mnemonicSize = lenght.rawValue
        let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryCreateViewController") as? RecoveryCreateViewController {
            vc.mnemonicLength = lenght
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        selectLength()
    }

    @IBAction func btnPrint(_ sender: Any) {
        print("Print")
    }
}

extension OnBoardInfoViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return viewModel.items.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch OnBoardInfoSection(rawValue: section) {
        case .info:
            return viewModel.items.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch OnBoardInfoSection(rawValue: indexPath.section) {
        case .info:
            if let cell = tableView.dequeueReusableCell(withIdentifier: OnBoardInfoCell.identifier, for: indexPath) as? OnBoardInfoCell {
                cell.configure(model: viewModel.items[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
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
        switch SecuritySelectSection(rawValue: section) {
        default:
            return footerView(.none)
        }
    }

    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        switch SecuritySelectSection(rawValue: indexPath.section) {
        default:
            return indexPath
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        switch OnBoardInfoSection(rawValue: indexPath.section) {
        case .info:
            break
        default:
            break
        }
    }
}

extension OnBoardInfoViewController {

    func headerView(_ txt: String) -> UIView {

        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.clear
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 14.0, weight: .semibold)
        title.text = txt
        title.textColor = .white.withAlphaComponent(0.6)
        title.numberOfLines = 1

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor, constant: 10.0),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 30),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 30)
        ])

        return section
    }

    func footerView(_ type: FooterType) -> UIView {

        switch type {
        default:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        }
    }
}

extension OnBoardInfoViewController: DialogListViewControllerDelegate {
    func didSelectDialogIndex(_ index: Int, for sender: Int) {
        switch PhrasePrefs(rawValue: index) {
        case ._12:
            next(._12)
        case ._24:
            next(._24)
        case .none:
            break
        }
    }
}
