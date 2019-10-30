import Foundation
import UIKit
import PromiseKit

class VerifyMnemonicsViewController: UIViewController {

    @IBOutlet var content: VerifyMnemonicsView!
    var mnemonic: [Substring]!
    var selectionWordNumbers: [Int] = [Int](repeating: 0, count: 4)
    var expectedWordNumbers: [Int] = [Int](repeating: 0, count: 4)
    var questionCounter: Int = 0
    var questionPosition: Int = 0
    let numberOfSteps: Int = 4

    private var progressToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        expectedWordNumbers = generateRandomWordNumbers()
        newRandomWords()
        content.viewTitle.text = getTitle()
        updateProcessBar()
        navigationItem.titleView = content.progressBarView
        reload()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        progressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil, queue: .main, using: progress)
        for button in content.buttonsArray {
            button.addTarget(self, action: #selector(self.click), for: .touchUpInside)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = progressToken {
            NotificationCenter.default.removeObserver(token)
        }
        for button in content.buttonsArray {
            button.removeTarget(self, action: #selector(self.click), for: .touchUpInside)
        }
    }

    func progress(_ notification: Notification) {
        Guarantee().map(on: DispatchQueue.global(qos: .background)) { () -> UInt32 in
            let json = try JSONSerialization.data(withJSONObject: notification.userInfo!, options: [])
            let tor = try JSONDecoder().decode(Tor.self, from: json)
            return tor.progress
        }.done { progress in
            self.progressIndicator?.message = NSLocalizedString("id_tor_status", comment: "") + " \(progress)%"
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let pinController = segue.destination as? PinSetViewController {
            pinController.mode = .create
        }
    }

    @objc func click(_ sender: UIButton) {
        var selectedWord: String?
        for button in content.buttonsArray {
            button.isSelected = false
            if button == sender {
                button.isSelected = true
                selectedWord = button.titleLabel?.text
            }
        }
        if selectedWord != nil, selectedWord == String(mnemonic[questionPosition]) {
            if isComplete() {
                registerAndLogin(mnemonics: mnemonic.joined(separator: " "))
            } else {
                questionCounter += 1
                newRandomWords()
                reload()
                updateProcessBar()
                content.viewTitle.text = getTitle()
            }
        } else {
            Toast.show(NSLocalizedString("id_wrong_choice_check_your", comment: ""), timeout: Toast.LONG)
            navigationController?.popViewController(animated: true)
        }
    }

    func registerAndLogin(mnemonics: String) {
        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!
        firstly {
            self.startAnimating(message: NSLocalizedString("id_logging_in", comment: ""))
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate.disconnect()
        }.compactMap(on: bgq) {
            try appDelegate.connect()
        }.compactMap(on: bgq) {
            try getSession().registerUser(mnemonic: mnemonics)
        }.then(on: bgq) { call in
            call.resolve(self)
        }.compactMap(on: bgq) { _ in
            try getSession().login(mnemonic: mnemonics)
        }.then(on: bgq) { call in
            call.resolve(self)
        }.then { _ in
            Registry.shared.refresh().recover { _ in Guarantee() }
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.performSegue(withIdentifier: "next", sender: self)
        }.catch { error in
            let message: String
            if let err = error as? GaError, err != GaError.GenericError {
                message = NSLocalizedString("id_you_are_not_connected_to_the", comment: "")
            } else {
                message = NSLocalizedString("id_login_failed", comment: "")
            }
            Toast.show(message, timeout: Toast.SHORT)
        }
    }

    func updateProcessBar() {
        content.processNodes[questionCounter].backgroundColor = UIColor.customMatrixGreen()
        content.processNodes[questionCounter].borderColor = UIColor.customMatrixGreen()

        if questionCounter > 0 {
            content.processConnectors[questionCounter - 1].backgroundColor = UIColor.customMatrixGreen()
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
        content.buttonsArray.enumerated().forEach { (offset, element) in
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
        content.textLabel.attributedText = attributedString
    }
}

@IBDesignable
class VerifyMnemonicsView: UIView {
    @IBOutlet weak var button0: DesignableButton!
    @IBOutlet weak var button1: DesignableButton!
    @IBOutlet weak var button2: DesignableButton!
    @IBOutlet weak var button3: DesignableButton!
    @IBOutlet weak var textLabel: UILabel!
    @IBOutlet weak var viewTitle: UILabel!
    @IBOutlet weak var processNode1: UIView!
    @IBOutlet weak var processNode2: UIView!
    @IBOutlet weak var processNode3: UIView!
    @IBOutlet weak var processNode4: UIView!
    @IBOutlet weak var progressBarConnector1: UIView!
    @IBOutlet weak var progressBarConnector2: UIView!
    @IBOutlet weak var progressBarConnector3: UIView!
    @IBOutlet weak var progressBarView: UIView!
    lazy var buttonsArray: [UIButton] = [button0, button1, button2, button3]
    lazy var processNodes: [UIView] = [processNode1, processNode2, processNode3, processNode4]
    lazy var processConnectors: [UIView] = [progressBarConnector1, progressBarConnector2, progressBarConnector3]

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }
}
