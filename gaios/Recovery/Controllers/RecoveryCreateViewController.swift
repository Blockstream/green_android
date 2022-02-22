import Foundation
import UIKit

class RecoveryCreateViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var progressView: UIProgressView!
    @IBOutlet weak var btnNext: UIButton!
    @IBOutlet weak var word1: UILabel!
    @IBOutlet weak var word2: UILabel!
    @IBOutlet weak var word3: UILabel!
    @IBOutlet weak var word4: UILabel!
    @IBOutlet weak var word5: UILabel!
    @IBOutlet weak var word6: UILabel!

    var subAccountCreateMnemonicLength: MnemonicLengthOption?

    lazy var arrayLabels: [UILabel] = [self.word1, self.word2, self.word3, self.word4, self.word5, self.word6]

    private var mnemonicSize: Int {
        if let subAccountCreateMnemonicLength = subAccountCreateMnemonicLength {
            return subAccountCreateMnemonicLength.rawValue
        }
        if OnBoardManager.shared.params?.mnemonicSize == MnemonicSize._24.rawValue {
            return MnemonicSize._24.rawValue
        }
        return MnemonicSize._12.rawValue
    }

    private var mnemonic: [Substring]!

    private var pageCounter = 0

    override func viewDidLoad() {
        super.viewDidLoad()

        mnemonicCreate()

        let newBackButton = UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItem.Style.plain, target: self, action: #selector(RecoveryCreateViewController.back(sender:)))
        navigationItem.leftBarButtonItem = newBackButton
        navigationItem.hidesBackButton = true

        setContent()
        setStyle()

        view.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.view
        word1.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.word1Lbl
        word2.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.word2Lbl
        word3.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.word3Lbl
        word4.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.word4Lbl
        word5.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.word5Lbl
        word6.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.word6Lbl
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryCreateScreen.nextBtn
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_write_down_the_words", comment: "")
        lblHint.text = NSLocalizedString("id_write_down_your_recovery_phrase", comment: "")
        btnNext.setTitle(NSLocalizedString("id_next", comment: ""), for: .normal)
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(true)

        pageCounter = 0
        loadWords()
    }

    @objc func back(sender: UIBarButtonItem) {
        if pageCounter == 0 {
            navigationController?.popViewController(animated: true)
        } else {
            pageCounter -= 1
            loadWords()
        }
    }

    func mnemonicCreate() {
        if mnemonicSize == MnemonicSize._24.rawValue {
            mnemonic = try! generateMnemonic().split(separator: " ")
        } else {
            mnemonic = try! generateMnemonic12().split(separator: " ")
        }
    }

    func loadWords() {
        progressView.progress = Float(pageCounter + 1) / Float((mnemonicSize / Constants.wordsPerPage))
        let start = pageCounter * Constants.wordsPerPage
        let end = start + Constants.wordsPerPage
        for index in start..<end {
            let real = index+1
            let formattedString = NSMutableAttributedString(string: String("\(real) \(mnemonic[index])"))
            formattedString.setColor(color: UIColor.customMatrixGreen(), forText: String(format: "%d", real))
            formattedString.setFont(font: UIFont.systemFont(ofSize: 18), stringValue: String(format: "%d", real))
            arrayLabels[index % Constants.wordsPerPage].attributedText = formattedString
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        if pageCounter == (mnemonicSize / Constants.wordsPerPage) - 1 {
            let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryVerifyViewController") as? RecoveryVerifyViewController {
                vc.mnemonic = mnemonic
                vc.subAccountCreateMnemonicLength = subAccountCreateMnemonicLength
                navigationController?.pushViewController(vc, animated: true)
            }
        } else {
            pageCounter += 1
            loadWords()
        }
    }

}
