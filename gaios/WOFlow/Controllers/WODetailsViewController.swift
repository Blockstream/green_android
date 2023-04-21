import UIKit

class WODetailsViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var segment: UISegmentedControl!
    @IBOutlet weak var textView: UITextView!
    @IBOutlet weak var bgTextView: UIView!
    @IBOutlet weak var btnPaste: UIButton!
    @IBOutlet weak var btnScan: UIButton!
    @IBOutlet weak var lblDesc: UILabel!
    @IBOutlet weak var btnImport: UIButton!
    @IBOutlet weak var lblCoda: UILabel!
    @IBOutlet weak var btnFile: UIButton!
    @IBOutlet weak var lblError: UILabel!
    @IBOutlet weak var bioView: UIView!
    @IBOutlet weak var iconBio: UIImageView!
    @IBOutlet weak var btnBio: UIButton!
    @IBOutlet weak var lblBio: UILabel!
    
    let viewModel = WOSelectViewModel()
    var isBio: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()

        textView.delegate = self
        textView.addDoneButtonToKeyboard(myAction: #selector(self.textView.resignFirstResponder))
        textView.textContainer.maximumNumberOfLines = 10
        textView.textContainer.heightTracksTextView = true
        textView.isScrollEnabled = false

        refresh()
    }

    func setContent() {
        lblTitle.text = "id_watchonly_details".localized
        lblHint.text = "".localized
        segment.setTitle("id_xpub".localized, forSegmentAt: 0)
        segment.setTitle("Descriptor".localized, forSegmentAt: 1)
        let font = UIFont.systemFont(ofSize: 14.0, weight: .medium)
        segment.setTitleTextAttributes([NSAttributedString.Key.font: font], for: .normal)
        lblCoda.text = "Watch-only wallets let you receive funds and check your balance."
        btnImport.setTitle("Import", for: .normal)
        lblError.text = "This is not a valid descriptor"
        btnFile.setTitle("Import from file", for: .normal)
        lblBio.text = NSLocalizedString("id_login_with_biometrics", comment: "")
    }

    func setStyle() {
        lblTitle.setStyle(.title)
        lblHint.setStyle(.txt)
        lblError.setStyle(.err)
        [lblDesc, lblCoda].forEach{
            $0?.setStyle(.txt)
            $0?.textColor = UIColor.gW40()
        }
        bgTextView.cornerRadius = 5.0
        btnImport.setStyle(.primaryDisabled)
        btnFile.setStyle(.inline)
        bioView.borderWidth = 1.0
        bioView.borderColor = .white.withAlphaComponent(0.7)
        bioView.layer.cornerRadius = 5.0
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.white], for: .selected)
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.lightGray], for: .normal)
    }

    @objc func onTextChange() {
        refresh()
    }

    func refresh() {
        lblError.isHidden = true
        btnImport.setStyle(textView.text.count > 2 ? .primary : .primaryDisabled)

        switch segment.selectedSegmentIndex {
        case 0:
            lblDesc.text = "id_scan_or_paste_your_extended".localized
        case 1:
            lblDesc.text = "Scan or paste your public descriptor to log in to your watch-only account.".localized
        default:
            break
        }
        iconBio.image = isBio ? UIImage(named: "ic_checkbox_on")! : UIImage(named: "ic_checkbox_off")!
    }

    func openDocumentPicker() {
        let documentPicker = UIDocumentPickerViewController(forOpeningContentTypes: [.text])
        documentPicker.delegate = self
        documentPicker.allowsMultipleSelection = false
        documentPicker.modalPresentationStyle = .automatic
        present(documentPicker, animated: true)
    }

    @IBAction func btnFile(_ sender: Any) {
        openDocumentPicker()
    }

    @IBAction func segment(_ sender: UISegmentedControl) {
        refresh()
    }

    @IBAction func btnPaste(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            textView.text = txt
        }
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }
    
    @IBAction func btnScan(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogScanViewController") as? DialogScanViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnBio(_ sender: Any) {
        isBio = !isBio
        refresh()
    }

    @IBAction func btnImport(_ sender: Any) {
        print("Done")
    }
}

extension WODetailsViewController: UITextViewDelegate {
    func textViewDidChange(_ textView: UITextView) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.onTextChange), object: nil)
        perform(#selector(self.onTextChange), with: nil, afterDelay: 0.5)
    }

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        if text == "\n" {
            textView.resignFirstResponder()
            return false
        }
        return true
    }
}

extension WODetailsViewController: DialogScanViewControllerDelegate {
    func didScan(value: String, index: Int?) {
        textView.text = value
        refresh()
    }
    func didStop() {
        //
    }
}

extension WODetailsViewController: UIDocumentPickerDelegate {
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentAt url: URL) {
        dismiss(animated: true)
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        
        guard let url = urls.first else { return }
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }
        do {
            // Get the contents
            let contents = try String(contentsOfFile: url.path, encoding: .utf8)
            textView.text = contents
            refresh()
        }
        catch let error as NSError {
            print("\(error)")
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        //
    }
}
