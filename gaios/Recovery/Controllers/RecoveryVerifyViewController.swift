import Foundation
import UIKit
import PromiseKit

class RecoveryVerifyViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var textLabel: UILabel!

    @IBOutlet weak var button0: UIButton!
    @IBOutlet weak var button1: UIButton!
    @IBOutlet weak var button2: UIButton!
    @IBOutlet weak var button3: UIButton!

    @IBOutlet weak var pageControl: UIPageControl!

    lazy var buttonsArray: [UIButton] = [button0, button1, button2, button3]

    private var mnemonicSize: Int {
        if let subAccountCreateMnemonicLength = subAccountCreateMnemonicLength {
            return subAccountCreateMnemonicLength.rawValue
        }
        if OnBoardManager.shared.params?.mnemonicSize == MnemonicSize._24.rawValue {
            return MnemonicSize._24.rawValue
        }
        return MnemonicSize._12.rawValue
    }
    var mnemonic: [Substring]!
    var selectionWordNumbers: [Int] = [Int](repeating: 0, count: Constants.wordsPerQuiz)
    var expectedWordNumbers: [Int] = []
    var questionCounter: Int = 0
    var questionPosition: Int = 0
    var numberOfSteps: Int = 4

    var subAccountCreateMnemonicLength: MnemonicLengthOption?

    override func viewDidLoad() {
        super.viewDidLoad()
        expectedWordNumbers = generateExpectedWordNumbers()
        newRandomWords()

        pageControl.numberOfPages = numberOfSteps
        updatePageControl()
        reload()

        lblTitle.text = NSLocalizedString("id_recovery_phrase_check", comment: "")
        updateHint()

        buttonsArray.forEach {
            $0.setStyle(.outlinedWhite)
        }

        view.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryVerifyScreen.view
        button0.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryVerifyScreen.word0btn
        button1.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryVerifyScreen.word1btn
        button2.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryVerifyScreen.word2btn
        button3.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryVerifyScreen.word3btn
        textLabel.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryVerifyScreen.quizLbl

        AnalyticsManager.shared.recordView(.recoveryCheck, sgmt: AnalyticsManager.shared.ntwSgmt(AccountsManager.shared.current))
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        for button in buttonsArray {
            button.addTarget(self, action: #selector(self.click), for: .touchUpInside)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        for button in buttonsArray {
            button.removeTarget(self, action: #selector(self.click), for: .touchUpInside)
        }
    }

    @objc func click(_ sender: UIButton) {
        var selectedWord: String?
        for button in buttonsArray {
            button.isSelected = false
            if button == sender {
                button.isSelected = true
                selectedWord = button.titleLabel?.text
            }
        }
        if selectedWord != nil, selectedWord == String(mnemonic[questionPosition]) {
            if isComplete() {
                DispatchQueue.main.async {
                    if let subAccountCreateMnemonicLength = self.subAccountCreateMnemonicLength {
                        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
                        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountCreateSetNameViewController") as? AccountCreateSetNameViewController {
                            vc.accountType = .twoOfThree
                            vc.recoveryKeyType = .newPhrase(lenght: subAccountCreateMnemonicLength.rawValue)
                            vc.recoveryMnemonic = self.mnemonic.joined(separator: " ")
                            self.navigationController?.pushViewController(vc, animated: true)
                        }
                    } else {
                        OnBoardManager.shared.params?.mnemonic = self.mnemonic.joined(separator: " ")
                        let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
                        let vc = storyboard.instantiateViewController(withIdentifier: "RecoverySuccessViewController")
                        self.navigationController?.pushViewController(vc, animated: true)
                    }
                }
            } else {
                questionCounter += 1
                newRandomWords()
                reload()
                updatePageControl()
            }
        } else {
            DropAlert().warning(message: NSLocalizedString("id_wrong_choice_check_your", comment: ""), delay: 4)
            navigationController?.popViewController(animated: true)
        }
    }

    func updatePageControl() {
        pageControl.currentPage = questionCounter
    }

    func newRandomWords() {
        questionPosition = expectedWordNumbers[questionCounter]
        selectionWordNumbers = generateRandomWordNumbers()
        if !selectionWordNumbers.contains(questionPosition) {
            selectionWordNumbers[getIndexFromUniformUInt32(count: 3)] = questionPosition
        }
    }

    func getIndexFromUniformUInt32(count: Int) -> Int {
        return Int(try! getUniformUInt32(upper_bound: UInt32(count)))
    }

    func generateRandomWordNumbers() -> [Int] {
        var words: [Int] = [Int](repeating: 0, count: Constants.wordsPerQuiz)
        repeat {
            // mnemonic.endIndex is 12
            // words in in range 0...11
            words = words.map { (_) -> Int in getIndexFromUniformUInt32(count: mnemonic.endIndex) }
        } while Set(words).count != Constants.wordsPerQuiz
        return words
    }

    func generateExpectedWordNumbers() -> [Int] {
        var words: [Int] = [Int](repeating: 0, count: numberOfSteps)
        repeat {
            words = words.map { (_) -> Int in getIndexFromUniformUInt32(count: mnemonic.endIndex) }
        } while Set(words).count != numberOfSteps
        return words
    }

    func updateHint() {
        lblHint.text = "What is word number \(questionPosition + 1)?"
    }

    func isComplete() -> Bool {
        return questionCounter == numberOfSteps - 1
    }

    func reload() {
        // update buttons
        buttonsArray.enumerated().forEach { (offset, element) in
            let word = String(mnemonic[selectionWordNumbers[offset]])
            element.setTitle(word, for: .normal)
            element.isSelected = false
            #if DEBUG
            element.isSelected = mnemonic[questionPosition] == word
            #endif
        }
        // update subtitle
        let rangeStart: Int
        let rangeEnd: Int
        if questionPosition == 0 {
            rangeStart = 0
            rangeEnd = 2
        } else if questionPosition == mnemonic.endIndex - 1 {
            rangeStart = (mnemonic.endIndex - 1) - 2
            rangeEnd = mnemonic.endIndex - 1
        } else {
            rangeStart = questionPosition - 1
            rangeEnd = questionPosition + 1
        }

        let question = "  ______   "
//        var str = ""
        let attributedString = NSMutableAttributedString(string: "")
        for idx in rangeStart...rangeEnd {
            if mnemonic[questionPosition] == mnemonic[idx] {
                attributedString.append(NSMutableAttributedString(string: question))
            } else {
                let prefix = "\(idx + 1)."
                attributedString.append(NSMutableAttributedString(string: "\(prefix) \(mnemonic[idx]) "))
                attributedString.setColor(color: UIColor.customMatrixGreen(), forText: question)
                attributedString.setColor(color: UIColor.customMatrixGreen(), forText: prefix)
            }
        }
        textLabel.attributedText = attributedString
        updateHint()
    }
}
