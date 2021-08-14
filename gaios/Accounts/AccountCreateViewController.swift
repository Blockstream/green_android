import UIKit
import PromiseKit

class AccountCreateViewController: UIViewController {

    @IBOutlet weak var networkIconImageView: UIImageView!
    @IBOutlet weak var networkNameLabel: UILabel!
    @IBOutlet weak var headerLabel: UILabel!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var nextButton: UIButton!

    public var canCreateAdvanced = true
    public weak var subaccountDelegate: SubaccountDelegate?
    private let accountTypes = AccountType.allCases
    private let accountInfoType = AccountInfoType.allCases
    private var account = AccountsManager.shared.current
    private var selectedAccountType: AccountType?
    private var isReview: Bool = false
    private var accountName: String?

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if let network = account?.gdkNetwork {
            networkNameLabel.text = network.name
            networkIconImageView.image = UIImage(named: network.icon!)
        }
        configureView()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.estimatedRowHeight = 80
        tableView.rowHeight = UITableView.automaticDimension
        tableView.dataSource = self
        tableView.delegate = self
        tableView.selectRow(at: IndexPath.init(row: 0, section: 0), animated: true, scrollPosition: .none)
    }

    func configureView() {
        if isReview {
            headerLabel.text = NSLocalizedString("id_review_account_information", comment: "")
            nextButton.setTitle(NSLocalizedString("id_add_new_account", comment: ""), for: .normal)
        } else {
            nextButton.backgroundColor = UIColor.customTitaniumLight()
            nextButton.isUserInteractionEnabled = false
            headerLabel.text = NSLocalizedString("id_what_type_of_account_would_you", comment: "")
            nextButton.setTitle(NSLocalizedString("id_next", comment: ""), for: .normal)
        }
        tableView.reloadData()
    }

    @IBAction func dismissButtonTapped(_ sender: Any) {
        dismissModal()
    }

    func dismissModal() {
        if #available(iOS 13.0, *) {
            if let presentationController = presentationController {
                presentationController.delegate?.presentationControllerDidDismiss?(presentationController)
            }
        }
        dismiss(animated: true, completion: nil)
    }

    @IBAction func nextButtonTapped(_ sender: Any) {
        if !isReview {
            isReview = true
            configureView()
        } else {
            if let name = accountName, !name.isEmpty, let type = selectedAccountType {
                createAccount(name: name, type: type)
            }
        }
    }

    func createAccount(name: String, type: AccountType) {
        let bgq = DispatchQueue.global(qos: .background)
        let session = SessionManager.shared
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try session.createSubaccount(details: ["name": name, "type": type.rawValue])
        }.then(on: bgq) { call in
            call.resolve()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.dismissModal()
        }.catch { e in
            DropAlert().error(message: e.localizedDescription)
            print(e.localizedDescription)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let next = segue.destination as? AccountInfoViewController {
            next.transitioningDelegate = self
            next.modalPresentationStyle = .custom
            if let accountInfoType = sender as? AccountInfoType {
                next.accountInfoType = accountInfoType
            }
        }
    }
}

extension AccountCreateViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return isReview ? 3 : accountTypes.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = isReview ? tableView.dequeueReusableCell(withIdentifier: "ReviewCell") as? ReviewCell : tableView.dequeueReusableCell(withIdentifier: "AccountTypeCell") as? AccountTypeCell {
            if let accountType = isReview ? selectedAccountType : accountTypes[indexPath.row] {
                cell.configure(for: accountType, indexPath: indexPath, delegate: self)
//                if accountType == .advanced {
//                    cell.selectable(canCreateAdvanced)
//                } else {
//                    cell.selectable(true)
//                }
                return cell
            }
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
//        if let cell = tableView.cellForRow(at: indexPath) as? AccountTypeCell {
//            if cell.accountType == .advanced && !canCreateAdvanced {
//                cell.accessoryType = .none
//                cell.delegate?.didTapInfo(for: .advanced)
//            } else if !isReview {
//                cell.accessoryType = .checkmark
//                selectedAccountType = accountTypes[indexPath.row]
//                nextButton.backgroundColor = UIColor.customMatrixGreen()
//                nextButton.isUserInteractionEnabled = true
//            }
//        }
    }

    func tableView(_ tableView: UITableView, didDeselectRowAt indexPath: IndexPath) {
        if let cell = tableView.cellForRow(at: indexPath) {
            cell.accessoryType = .none
        }
    }
}

extension AccountCreateViewController: UIViewControllerTransitioningDelegate {
    func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        return ModalPresentationController(presentedViewController: presented, presenting: presenting)
    }

    func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        ModalAnimator(isPresenting: true)
    }

    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        ModalAnimator(isPresenting: false)
    }
}

extension AccountCreateViewController: AccountTypeInfoDelegate {
    func didTapInfo(for accountInfoType: AccountInfoType) {
        performSegue(withIdentifier: "info", sender: accountInfoType)
    }

    func didChange(_ name: String) {
        accountName = name
        nextButton.isUserInteractionEnabled = true
    }
}
