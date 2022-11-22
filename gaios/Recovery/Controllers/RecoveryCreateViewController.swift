import Foundation
import UIKit

class RecoveryCreateViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblNote: UILabel!
    @IBOutlet weak var pageControl: UIPageControl!
    @IBOutlet weak var btnNext: UIButton!
    @IBOutlet weak var word1: UILabel!
    @IBOutlet weak var word2: UILabel!
    @IBOutlet weak var word3: UILabel!
    @IBOutlet weak var word4: UILabel!
    @IBOutlet weak var word5: UILabel!
    @IBOutlet weak var word6: UILabel!

    var mnemonicLength: MnemonicLengthOption?

    lazy var arrayLabels: [UILabel] = [self.word1, self.word2, self.word3, self.word4, self.word5, self.word6]

    private var mnemonicSize: Int {
        return mnemonicLength?.rawValue ?? MnemonicSize._12.rawValue
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
        pageControl.numberOfPages = mnemonicSize / Constants.wordsPerPage

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
        let title = "Write down the order and your recovery phrase"
        let strs = ["order", "recovery phrase"]

        let attributedText = NSMutableAttributedString.init(string: title)
        for str1 in strs {
            let range = (title as NSString).range(of: str1)
            attributedText.addAttribute(NSAttributedString.Key.foregroundColor, value: UIColor.gGreenMatrix(), range: range)
            lblTitle.attributedText = attributedText
        }

        lblHint.text = "Store it somewhere safe."
        btnNext.setTitle(NSLocalizedString("id_next", comment: ""), for: .normal)
        lblNote.text = "Make sure to be in a private and safe space"
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
        pageControl.currentPage = pageCounter

        let start = pageCounter * Constants.wordsPerPage
        let end = start + Constants.wordsPerPage
        for index in start..<end {
            let real = index+1
            let formattedString = NSMutableAttributedString(string: String("\(real) \(mnemonic[index])"))
            formattedString.setColor(color: UIColor.gGreenMatrix(), forText: String(format: "%d", real))
            formattedString.setFont(font: UIFont.systemFont(ofSize: 16, weight: .semibold), stringValue: String(format: "%d", real))
            arrayLabels[index % Constants.wordsPerPage].attributedText = formattedString
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        if pageCounter == (mnemonicSize / Constants.wordsPerPage) - 1 {
            let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryVerifyViewController") as? RecoveryVerifyViewController {
                vc.mnemonic = mnemonic
                navigationController?.pushViewController(vc, animated: true)
            }
        } else {
            pageCounter += 1
            loadWords()
        }
    }

}
