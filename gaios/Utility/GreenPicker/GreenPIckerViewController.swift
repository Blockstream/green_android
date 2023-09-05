import Foundation
import UIKit

protocol GreenPickerDelegate: AnyObject {
    func didSelectItem(_ idx: Int)
    func didCancel()
}

enum GreenPickerAction {
    case select(idx: Int)
    case cancel
}

class GreenPickerViewController: UIViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var searchField: UITextField!
    
    var vm: GreenPickerViewModel!
    weak var delegate: GreenPickerDelegate?

    lazy var blurredView: UIView = {
        let containerView = UIView()
        let blurEffect = UIBlurEffect(style: .dark)
        let customBlurEffectView = CustomVisualEffectView(effect: blurEffect, intensity: 0.4)
        customBlurEffectView.frame = self.view.bounds

        let dimmedView = UIView()
        dimmedView.backgroundColor = .black.withAlphaComponent(0.3)
        dimmedView.frame = self.view.bounds
        containerView.addSubview(customBlurEffectView)
        containerView.addSubview(dimmedView)
        return containerView
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        register()
        setContent()
        setStyle()
        searchField.delegate = self
        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)

        view.alpha = 0.0
        anchorBottom.constant = -cardView.frame.size.height

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTap))
            tappableBg.addGestureRecognizer(tapToClose)
    }

    deinit {
        print("deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        anchorBottom.constant = 0
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
            self.view.layoutIfNeeded()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func setContent() {
        lblTitle.text = vm.title
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5
        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
    }

    func register() {
        ["GreenPickerCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
    }

    @objc func triggerTextChange() {
        vm?.search(searchField.text ?? "")
        tableView.reloadData()
    }

    func dismiss(_ action: GreenPickerAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            switch action {
            case .cancel:
                break
            case .select(let idx):
                self.delegate?.didSelectItem(self.vm?.getIdx(idx) ?? 0)
            }
            self.dismiss(animated: false, completion: nil)
        })
    }

    @objc func didTap(gesture: UIGestureRecognizer) {
        dismiss(.cancel)
    }

    @objc func didSwipe(gesture: UIGestureRecognizer) {

        if let swipeGesture = gesture as? UISwipeGestureRecognizer {
            switch swipeGesture.direction {
            case .down:
                dismiss(.cancel)
            default:
                break
            }
        }
    }

    @IBAction func onEditingChange(_ sender: Any) {
        triggerTextChange()
    }
}

extension GreenPickerViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return vm.filteredItems.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: GreenPickerCell.identifier) as? GreenPickerCell {
            cell.selectionStyle = .none
            let item = vm.filteredItems[indexPath.row]
            cell.configure(model: item, isSelected: false)
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        dismiss(.select(idx: indexPath.row))
    }
}

extension GreenPickerViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
    
    func textFieldDidBeginEditing(_ textField: UITextField) {}
    
    func textFieldDidEndEditing(_ textField: UITextField) {}
}
