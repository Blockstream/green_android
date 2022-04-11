import UIKit

enum MenuUnarchiveOption {
    case unarchive
}

extension MenuUnarchiveOption: CaseIterable {}

protocol PopoverMenuUnarchiveDelegate: AnyObject {
    func didSelectionMenuOption(option: MenuUnarchiveOption, index: Int)
}

class PopoverMenuUnarchiveViewController: UIViewController {

    @IBOutlet weak var menuTableView: UITableView!

    weak var delegate: PopoverMenuUnarchiveDelegate?
    private var isLiquid: Bool!
    private var menuOptions = MenuUnarchiveOption.allCases
    var index: Int?

    override func viewDidLoad() {
        super.viewDidLoad()
        menuTableView.delegate = self
        menuTableView.dataSource = self
        menuTableView.estimatedRowHeight = 44
        menuTableView.rowHeight = UITableView.automaticDimension
    }
}

extension PopoverMenuUnarchiveViewController: UITableViewDataSource, UITableViewDelegate {

    override var preferredContentSize: CGSize {
        get {
            return CGSize(width: super.preferredContentSize.width, height: menuTableView.rect(forSection: 0).height)
        }
        set {
            super.preferredContentSize = newValue
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return menuOptions.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "MenuOptionCell") {
            let option = menuOptions[indexPath.row]
            switch option {
            case .unarchive:
                cell.textLabel?.text = NSLocalizedString("id_unarchive", comment: "")
            }
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.dismiss(animated: true) {
            self.delegate?.didSelectionMenuOption(option: self.menuOptions[indexPath.row], index: self.index ?? 0)
        }
    }
}
