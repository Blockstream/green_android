import UIKit

enum MenuAccountOption {
    case rename
    case archive
}

extension MenuAccountOption: CaseIterable {}

protocol PopoverMenuAccountDelegate: AnyObject {
    func didSelectionMenuOption(option: MenuAccountOption, index: Int)
}

class PopoverMenuAccountViewController: UIViewController {

    @IBOutlet weak var menuTableView: UITableView!

    weak var delegate: PopoverMenuAccountDelegate?
    private var isLiquid: Bool!
    private var menuOptions = MenuAccountOption.allCases
    var index: Int?
    var canArchive: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        menuTableView.delegate = self
        menuTableView.dataSource = self
        menuTableView.estimatedRowHeight = 44
        menuTableView.rowHeight = UITableView.automaticDimension
    }
}

extension PopoverMenuAccountViewController: UITableViewDataSource, UITableViewDelegate {

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
        if canArchive {
            return menuOptions.count
        } else {
            return 1
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "MenuOptionCell") {
            let option = menuOptions[indexPath.row]
            switch option {
            case .rename:
                cell.textLabel?.text = NSLocalizedString("id_rename_account", comment: "")
            case .archive:
                cell.textLabel?.text = NSLocalizedString("id_archive", comment: "")
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
