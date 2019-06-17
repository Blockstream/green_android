import Foundation
import UIKit
import PromiseKit

class AssetTableViewController: UITableViewController, UITextViewDelegate {

    var tag: String!
    var asset: AssetInfo?
    var satoshi: UInt64?
    var negative: Bool = false

    private var isReadOnly = true
    private var assetTableCell: AssetTableCell?
    private var keyboardDismissGesture: UIGestureRecognizer?

    @IBOutlet weak var hexTitle: UILabel!
    @IBOutlet weak var hexLabel: UILabel!
    @IBOutlet weak var decimalsTitle: UILabel!
    @IBOutlet weak var decimalsSaveButton: UIButton!
    @IBOutlet weak var decimalsTextField: UITextField!
    @IBOutlet weak var tickerTitle: UILabel!
    @IBOutlet weak var tickerSaveButton: UIButton!
    @IBOutlet weak var tickerTextField: UITextField!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSaveButton: UIButton!
    @IBOutlet weak var labelTextView: UITextView!
    @IBOutlet weak var assetCell: UITableViewCell!

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.tableFooterView = UIView()

        title = NSLocalizedString("id_asset_details", comment: "")
        hexTitle.text = NSLocalizedString("id_asset_id", comment: "").uppercased()
        decimalsTitle.text = NSLocalizedString("id_precision", comment: "").uppercased()
        tickerTitle.text = NSLocalizedString("id_ticker", comment: "").uppercased()
        labelTitle.text = NSLocalizedString("id_name", comment: "").uppercased()
        decimalsSaveButton.setTitle(NSLocalizedString("id_save", comment: "").uppercased(), for: .normal)
        tickerSaveButton.setTitle(NSLocalizedString("id_save", comment: "").uppercased(), for: .normal)
        labelSaveButton.setTitle(NSLocalizedString("id_save", comment: "").uppercased(), for: .normal)

        assetTableCell = Bundle.main.loadNibNamed("AssetTableCell", owner: self, options: nil)!.first as? AssetTableCell
        assetCell.addSubview(assetTableCell!)

        hexLabel.text = tag
        decimalsTextField.text = String(asset?.precision ?? 0)
        tickerTextField.text = asset?.ticker ?? ""
        labelTextView.text = asset?.name ?? ""
        labelTextView.delegate = self
        decimalsTextField.isEnabled = !isReadOnly
        tickerTextField.isEnabled = !isReadOnly
        labelTextView.isEditable = !isReadOnly
        tickerSaveButton.isHidden = true
        labelSaveButton.isHidden = true
        decimalsSaveButton.isHidden = true
        header()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide), name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        assetTableCell?.frame = assetCell.bounds
        assetTableCell?.setNeedsLayout()
    }

    @objc func keyboardWillShow(notification: NSNotification) {
        if keyboardDismissGesture == nil {
            keyboardDismissGesture = UITapGestureRecognizer(target: self, action: #selector(KeyboardViewController.dismissKeyboard))
            view.addGestureRecognizer(keyboardDismissGesture!)
        }
    }

    @objc func keyboardWillHide(notification: NSNotification) {
        if keyboardDismissGesture != nil {
            view.removeGestureRecognizer(keyboardDismissGesture!)
            keyboardDismissGesture = nil
        }
    }

    @objc func dismissKeyboard() {
        view.endEditing(true)
    }

    func textViewDidChangeSelection(_ textView: UITextView) {
        if isReadOnly { return }
        labelSaveButton.isHidden = false
        let text = labelTextView.text
        labelSaveButton.isEnabled = 5...256 ~= text!.count
    }

    @IBAction func decimalsChanged(_ sender: Any) {
        decimalsSaveButton.isHidden = false
        guard let number = UInt8(decimalsTextField.text!) else {
            return
        }
        decimalsSaveButton.isEnabled = 0...13 ~= number
    }

    @IBAction func tickerChanged(_ sender: Any) {
        tickerSaveButton.isHidden = false
        let text = tickerTextField.text
        tickerSaveButton.isEnabled = 3...4 ~= text!.count
    }

    @IBAction func decimalsSaveClick(_ sender: Any) {
        guard let number = UInt8(decimalsTextField.text!) else {
            return
        }
        asset?.precision = number
        decimalsSaveButton.isHidden = true
        header()
        dismissKeyboard()
    }

    @IBAction func tickerSaveClick(_ sender: Any) {
        asset?.ticker = tickerTextField.text!
        tickerSaveButton.isHidden = true
        header()
        dismissKeyboard()
    }

    @IBAction func labelSaveClick(_ sender: Any) {
        asset?.name = labelTextView.text!
        labelSaveButton.isHidden = true
        header()
        dismissKeyboard()
    }

    func header() {
        assetTableCell?.setup(tag: tag, asset: asset, satoshi: satoshi ?? 0, negative: negative)
    }
}
