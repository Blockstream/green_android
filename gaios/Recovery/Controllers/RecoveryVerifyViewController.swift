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

    var mnemonic: [Substring]!
    var selectionWordNumbers: [Int] = [Int](repeating: 0, count: Constants.wordsPerQuiz)
    var expectedWordNumbers: [Int] = []
    var questionCounter: Int = 0
    var questionPosition: Int = 0
    var numberOfSteps: Int = 4

    override func viewDidLoad() {
        super.viewDidLoad()
        expectedWordNumbers = generateExpectedWordNumbers()
        newRandomWords()

        customBack()
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

        AnalyticsManager.shared.recordView(.recoveryCheck, sgmt: AnalyticsManager.shared.ntwSgmtUnified())
    }

    func customBack() {
        var arrow = UIImage.init(named: "backarrow")
        if #available(iOS 13.0, *) {
            arrow = UIImage(systemName: "chevron.backward")
        }
        let newBackButton = UIBarButtonItem(image: arrow, style: UIBarButtonItem.Style.plain, target: self, action: #selector(RecoveryVerifyViewController.back(sender:)))
        navigationItem.leftBarButtonItem = newBackButton
        navigationItem.hidesBackButton = true
    }

    @objc func back(sender: UIBarButtonItem) {
        navigationController?.popViewController(animated: true)
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
                    self.next()
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

    func next() {
        if OnBoardInfoViewController.flowType == .onboarding {
            createWallet()
        } else {
            self.navigationController?.popToViewController(ofClass: AccountCreateRecoveryKeyViewController.self, animated: false)
            OnBoardInfoViewController.delegate?.didNewRecoveryPhrase(self.mnemonic.joined(separator: " "))
        }
    }

    func createWallet() {
        let testnet = LandingViewController.chainType == .testnet
        let name = AccountsManager.shared.getUniqueAccountName(testnet: testnet)
        let mainNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let account = Account(name: name, network: mainNetwork.network)
        let wm = WalletManager.getOrAdd(for: account)
        let mnemonic = self.mnemonic.joined(separator: " ")
        let credentials = Credentials(mnemonic: mnemonic, password: "")
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
            .compactMap { self.startLoader(message: NSLocalizedString("id_creating_wallet", comment: "")) }
            .then(on: bgq) { wm.create(credentials) }
            .compactMap { AccountsManager.shared.current = account }
            .then(on: bgq) { wm.login(credentials) }
            .ensure { self.stopLoader() }
            .done {
                AnalyticsManager.shared.createWallet(account: AccountsManager.shared.current)
                let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController") as? SetPinViewController {
                    vc.pinFlow = .onboard
                    self.navigationController?.pushViewController(vc, animated: true)
                }
            }.catch { err in self.showError(err) }
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

        let question = " ______   "
//        var str = ""
        let attributedString = NSMutableAttributedString(string: "")
        for idx in rangeStart...rangeEnd {

            let prefix = "\(idx + 1)."
            if mnemonic[questionPosition] == mnemonic[idx] {
                attributedString.append(NSMutableAttributedString(string: " \(prefix)\(question)"))
            } else {
                attributedString.append(NSMutableAttributedString(string: "\(prefix) \(mnemonic[idx]) "))
            }
            attributedString.setColor(color: UIColor.customMatrixGreen(), forText: question)
            attributedString.setColor(color: UIColor.customMatrixGreen(), forText: prefix)
        }
        textLabel.attributedText = attributedString
        updateHint()
    }
}
