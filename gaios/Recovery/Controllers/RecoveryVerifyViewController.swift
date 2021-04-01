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

    @IBOutlet weak var progressBarView: UIView!

    @IBOutlet weak var processNode1: UIView!
    @IBOutlet weak var processNode2: UIView!
    @IBOutlet weak var processNode3: UIView!
    @IBOutlet weak var processNode4: UIView!
    @IBOutlet weak var progressBarConnector1: UIView!
    @IBOutlet weak var progressBarConnector2: UIView!
    @IBOutlet weak var progressBarConnector3: UIView!

    lazy var buttonsArray: [UIButton] = [button0, button1, button2, button3]
    lazy var processNodes: [UIView] = [processNode1, processNode2, processNode3, processNode4]
    lazy var processConnectors: [UIView] = [progressBarConnector1, progressBarConnector2, progressBarConnector3]

    var mnemonic: [Substring]!
    var selectionWordNumbers: [Int] = [Int](repeating: 0, count: 4)
    var expectedWordNumbers: [Int] = [Int](repeating: 0, count: 4)
    var questionCounter: Int = 0
    var questionPosition: Int = 0
    let numberOfSteps: Int = 4

    override func viewDidLoad() {
        super.viewDidLoad()
        expectedWordNumbers = generateRandomWordNumbers()
        newRandomWords()

        updateProcessBar()
        navigationItem.titleView = progressBarView
        reload()

        lblTitle.text = "Recovery Phrase Check"
        lblHint.text = "Write these words down, and save them in a safe place. We’ll test you at the end of it."

        for btn in buttonsArray {
            btn.setTitleColor(.white, for: .normal)
            btn.borderWidth = 2.0
            btn.borderColor = UIColor.customGrayLight()
            btn.layer.cornerRadius = 4.0
        }
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
                    OnBoardManager.shared.params?.mnemonic = self.mnemonic.joined(separator: " ")
                    let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
                    let vc = storyboard.instantiateViewController(withIdentifier: "RecoverySuccessViewController")
                    self.navigationController?.pushViewController(vc, animated: true)
                }
            } else {
                questionCounter += 1
                newRandomWords()
                reload()
                updateProcessBar()
//                viewTitle.text = getTitle()
            }
        } else {
            DropAlert().warning(message: NSLocalizedString("id_wrong_choice_check_your", comment: ""), delay: 4)
            navigationController?.popViewController(animated: true)
        }
    }

    func updateProcessBar() {
        processNodes[questionCounter].backgroundColor = UIColor.customMatrixGreen()
        processNodes[questionCounter].borderColor = UIColor.customMatrixGreen()

        if questionCounter > 0 {
            processConnectors[questionCounter - 1].backgroundColor = UIColor.customMatrixGreen()
        }
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
        var words: [Int] = [Int](repeating: 0, count: 4)
        repeat {
            words = words.map { (_) -> Int in getIndexFromUniformUInt32(count: 23) }
        } while Set(words).count != 4
        return words
    }

    func getTitle() -> String {
        let localized = NSLocalizedString("id_select_word_number_d", comment: "")
        return String(format: localized, questionPosition + 1)
    }

    func isComplete() -> Bool {
        return questionCounter == numberOfSteps - 1
    }

    func reload() {
        // update buttons
        buttonsArray.enumerated().forEach { (offset, element) in
            element.setTitle(String(mnemonic[selectionWordNumbers[offset]]), for: .normal)
            element.isSelected = false
        }
        // update subtitle
        let rangeStart: Int
        let rangeEnd: Int
        if questionPosition == 0 {
            rangeStart = 0
            rangeEnd = 2
        } else if questionPosition == 23 {
            rangeStart = 21
            rangeEnd = 23
        } else {
            rangeStart = questionPosition - 1
            rangeEnd = questionPosition + 1
        }
        let question = "  ______   "
        let placeHolder = mnemonic[rangeStart...rangeEnd].joined(separator: " ").replacingOccurrences(of: mnemonic[questionPosition], with: question)
        let attributedString = NSMutableAttributedString(string: placeHolder)
        attributedString.setColor(color: UIColor.customMatrixGreen(), forText: question)
        textLabel.attributedText = attributedString
    }
}
