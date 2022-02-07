import UIKit
import PromiseKit

enum RecoveryType {
    case qr
    case phrase
}

class MnemonicViewController: KeyboardViewController, SuggestionsDelegate {

    @IBOutlet weak var doneButton: UIButton!

    @IBOutlet weak var mnemonicWords: UICollectionView!
    @IBOutlet weak var passwordProtectedSwitch: UISwitch!
    @IBOutlet weak var passwordProtectedLabel: UILabel!
    @IBOutlet weak var header: UIView!
    @IBOutlet weak var lblTitle: UILabel!
    let helpButton = UIButton(type: .system)

    let WL = getBIP39WordList()

    var suggestions: KeyboardSuggestions?
    var mnemonic = [String](repeating: String(), count: 27)
    var qrCodeReader: QRCodeReaderView?
    var isPasswordProtected = false {
        willSet {
            passwordProtectedSwitch.isOn = newValue
        }
    }

    var currIndexPath: IndexPath?
    var recoveryType = RecoveryType.qr

    override func viewDidLoad() {
        super.viewDidLoad()

        title = ""
        updateLblTitle()
        doneButton.setTitle(NSLocalizedString("id_restore", comment: ""), for: .normal)
        passwordProtectedLabel.text = NSLocalizedString("id_password_protected", comment: "")

        mnemonicWords.delegate = self
        mnemonicWords.dataSource = self
        updateDoneButton(false)

        createSuggestionView()

        switch recoveryType {
        case .qr:
            startScan()
        case .phrase:
            updateNavigationItem()
        }

        view.accessibilityIdentifier = AccessibilityIdentifiers.MnemonicScreen.view
        lblTitle.accessibilityIdentifier = AccessibilityIdentifiers.MnemonicScreen.titleLbl
        doneButton.accessibilityIdentifier = AccessibilityIdentifiers.MnemonicScreen.doneBtn
    }

    func updateNavigationItem() {
        helpButton.setImage(UIImage(named: "ic_help"), for: .normal)
        helpButton.addTarget(self, action: #selector(helpButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: helpButton)
    }

    @objc func helpButtonTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogRecoveryHelpViewController") as? DialogRecoveryHelpViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

        let contentInset = UIEdgeInsets.zero
        mnemonicWords.contentInset = contentInset
        mnemonicWords.scrollIndicatorInsets = contentInset
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        doneButton.updateGradientLayerFrame()
    }

    func updateLblTitle() {
        switch recoveryType {
        case .qr:
            lblTitle.text = NSLocalizedString("id_scan_qr_code", comment: "")
        case .phrase:
            lblTitle.text = NSLocalizedString("id_enter_your_recovery_phrase", comment: "")
        }
    }

    func updateDoneButton(_ enable: Bool) {
        doneButton.isEnabled = enable
        if enable {
            doneButton.backgroundColor = UIColor.customMatrixGreen()
            doneButton.setTitleColor(.white, for: .normal)
        } else {
            doneButton.backgroundColor = UIColor.customBtnOff()
            doneButton.setTitleColor(UIColor.customGrayLight(), for: .normal)
        }
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        let contentInset = UIEdgeInsets(top: 0.0, left: 0.0, bottom: keyboardFrame.height, right: 0.0)
        mnemonicWords.contentInset = contentInset
        mnemonicWords.scrollIndicatorInsets = contentInset
        suggestions!.frame = CGRect(x: 0, y: view.frame.height - keyboardFrame.height - 40, width: view.frame.width, height: 40)
    }

    override func keyboardWillHide(notification: Notification) {
        suggestions!.isHidden = true
        super.keyboardWillHide(notification: notification)
    }

    func createSuggestionView() {
        suggestions = KeyboardSuggestions(frame: CGRect(x: 0, y: 0, width: view.frame.width, height: 40))
        suggestions!.suggestionDelegate = self
        suggestions!.isHidden = true
        view.addSubview(suggestions!)
    }

    func suggestionWasTapped(suggestion: String) {
        if let currIndexPath = currIndexPath {
            mnemonic[currIndexPath.row + currIndexPath.section * 3] = suggestion
            mnemonicWords.reloadItems(at: [currIndexPath])

            let nextRow = (currIndexPath.row + 1) % 3
            let nextSection = nextRow == 0 ? (currIndexPath.section + 1) % (isPasswordProtected ? 9 : 8) : currIndexPath.section
            let nextIndexPath = IndexPath(row: nextRow, section: nextSection)
            if let cell = mnemonicWords.cellForItem(at: nextIndexPath) as? MnemonicCell {
                mnemonicWords.selectItem(at: nextIndexPath, animated: false, scrollPosition: UICollectionView.ScrollPosition.centeredVertically)
                cell.wordText.becomeFirstResponder()
                suggestions?.isHidden = true
            }
        }
    }

    func getMnemonicString() -> Promise<(String, String)> {
        return Promise { seal in
            if self.isPasswordProtected {
                let alert = UIAlertController(title: NSLocalizedString("id_encryption_passphrase", comment: ""), message: NSLocalizedString("id_please_provide_your_passphrase", comment: ""), preferredStyle: .alert)
                alert.addTextField { textField in
                    textField.keyboardType = .asciiCapable
                    textField.isSecureTextEntry = true
                }
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
                    seal.reject(GaError.GenericError)
                })
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_next", comment: ""), style: .default) { (_: UIAlertAction) in
                    let textField = alert.textFields![0]
                    seal.fulfill((self.mnemonic.prefix(upTo: 27).joined(separator: " ").lowercased(), textField.text!))
                })
                DispatchQueue.main.async {
                    self.present(alert, animated: true, completion: nil)
                }
            } else {
                seal.fulfill((mnemonic.prefix(upTo: 24).joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), String()))
            }
        }
    }

    func getSuggestions(prefix: String) -> [String] {
        return WL.filter { $0.hasPrefix(prefix) }
    }

    func updateSuggestions(prefix: String) {
        let words = getSuggestions(prefix: prefix)
        self.suggestions!.setSuggestions(suggestions: words)
        self.suggestions!.isHidden = words.isEmpty
    }

    enum LoginError: Error {
        case invalidMnemonic
    }

    fileprivate func validate(mnemonic: String, password: String) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startLoader(message: NSLocalizedString("id_setting_up_your_wallet", comment: ""))
            return Guarantee()
        }.compactMap(on: bgq) {
            guard try validateMnemonic(mnemonic: mnemonic) else {
                throw LoginError.invalidMnemonic
            }
        }.ensure {
            self.stopLoader()
        }.done { _ in
            OnBoardManager.shared.params?.mnemonic = mnemonic
            OnBoardManager.shared.params?.mnemomicPassword = password
            let storyboard = UIStoryboard(name: "AutomaticRestore", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "ExistingWalletsViewController")
            self.navigationController?.pushViewController(vc, animated: true)
        }.catch { error in
            DropAlert().error(message: "Invalid Recovery Phrase")
        }
    }

    @IBAction func doneButtonClicked(_ sender: Any) {
        _ = getMnemonicString()
            .done {
                self.validate(mnemonic: $0.0, password: $0.1)
            }
    }

    func checkTextfield(textField: UITextField) {
        if let text = textField.text {
            let suggestions = getSuggestions(prefix: text)
            textField.textColor = !suggestions.contains(text) ? UIColor.errorRed() : UIColor.white
        }
    }

    @IBAction func switchChanged(_ sender: UISwitch) {
        isPasswordProtected = sender.isOn
        mnemonicWords.reloadData()
        let foundEmpty = mnemonic.prefix(upTo: isPasswordProtected ? 27 : 24).contains(where: { $0.isEmpty })
        updateDoneButton(!foundEmpty)
    }

    func startScan() {
        if qrCodeReader == nil {
            qrCodeReader = QRCodeReaderView()
            let tap = UITapGestureRecognizer(target: self, action: #selector(onTap))
            qrCodeReader!.addGestureRecognizer(tap)
            qrCodeReader!.delegate = self
            qrCodeReader!.frame = CGRect(x: 0, y: 0, width: view.frame.width, height: view.frame.height)
        }
        if !qrCodeReader!.isSessionAuthorized() {
            qrCodeReader!.requestVideoAccess(presentingViewController: self)
            if !qrCodeReader!.isSessionAuthorized() {
                recoveryType = .phrase
                updateLblTitle()
                return
            }
        }
        view.insertSubview(qrCodeReader!, belowSubview: header)
        view.layoutIfNeeded()
        qrCodeReader!.startScan()
    }

    func stopScan() {
        qrCodeReader!.stopScan()
        qrCodeReader!.removeFromSuperview()
    }

    func onScan(mnemonic: String) {
        recoveryType = .phrase
        updateLblTitle()
        onPaste(mnemonic)
    }

    @objc func onTap(sender: UITapGestureRecognizer?) {

//        stopScan()
    }
}

extension MnemonicViewController: QRCodeReaderDelegate {
    func userDidGrant(_ granted: Bool) {
        DispatchQueue.main.async {
            if granted {
                self.startScan()
            } else {
                self.navigationController?.popViewController(animated: true)
            }
        }
    }

    private func onPaste(_ result: String) {
        let words = result.split(separator: " ")
        guard words.count == 12 || words.count == 24 || words.count == 27 else {
            return
        }

        words.enumerated().forEach { mnemonic[$0.0] = String($0.1) }
        isPasswordProtected = words.count == 27

        mnemonicWords.reloadData()
        updateDoneButton(true)
    }

    func onQRCodeReadSuccess(result: String) {
        onScan(mnemonic: result)
        stopScan()
    }
}

extension MnemonicViewController: UICollectionViewDelegate {

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "wordCell", for: indexPath) as? MnemonicCell else { fatalError("Fail to dequeue reusable cell") }
        cell.wordLabel.text = String(indexPath.row + indexPath.section * 3 + 1)
        cell.wordText.text = mnemonic[indexPath.row + indexPath.section * 3]
        checkTextfield(textField: cell.wordText)
        cell.delegate = self
        cell.wordText.accessibilityIdentifier = String(indexPath.row + indexPath.section * 3 + 1)
        return cell
    }
}

extension MnemonicViewController: UICollectionViewDataSource {

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return 3
    }

    func numberOfSections(in collectionView: UICollectionView) -> Int {
        return isPasswordProtected ? 9 : 8
    }
}

extension MnemonicViewController: UICollectionViewDelegateFlowLayout {

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        return CGSize(width: collectionView.frame.width / 3, height: 40)
    }
}

extension MnemonicViewController: MnemonicCellDelegate {

    func collectionView(valueChangedIn textField: UITextField, from cell: MnemonicCell) {
        let text = textField.text?.isEmpty ?? true ? String() : textField.text!

        currIndexPath = mnemonicWords.indexPath(for: cell)
        if currIndexPath == nil {
            return
        }

        mnemonic[currIndexPath!.row + currIndexPath!.section * 3] = text

        checkTextfield(textField: textField)
        if !text.isEmpty {
            updateSuggestions(prefix: textField.text!)
        } else {
            suggestions!.isHidden = true
        }

        // pass focus to next item for valid words of length >= 3
//        if text.count >= 3 && WL.contains(text) && WL.filter({ $0.contains(text) }).count == 1 {
//            suggestionWasTapped(suggestion: text)
//        }

        if text.count >= 3 {
            let filtered = WL.filter({ $0.hasPrefix(text) })
            if filtered.count == 1 {
                if filtered.first?.count == text.count {
                    suggestionWasTapped(suggestion: text)
                }
            }
        }

        let len = mnemonic.filter { !$0.isEmpty }.count
        updateDoneButton((isPasswordProtected && len == 27) || [12, 24].contains(len))
    }

    func collectionView(pastedIn text: String, from cell: MnemonicCell) {
        currIndexPath = mnemonicWords.indexPath(for: cell)
        onPaste(text)
    }
}

extension MnemonicViewController: DialogRecoveryHelpViewControllerDelegate {
    func didTapHelpCenter() {
        if let url = URL(string: "https://help.blockstream.com/hc/en-us/articles/900001388566-Why-is-my-mnemonic-backup-not-working-") {
            UIApplication.shared.open(url)
        }
    }

    func didCancel() {
        //
    }

}
