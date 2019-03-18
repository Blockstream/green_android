import Foundation
import UIKit

class CreateWalletViewController: UIViewController {

    @IBOutlet var content: CreateWalletView!
    private var mnemonic: [Substring] = {
        return try! generateMnemonic().split(separator: " ")
    }()
    private var pageCounter = 0;

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_write_down_the_words", comment: "")

        let newBackButton = UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItemStyle.plain, target: self, action: #selector(CreateWalletViewController.back(sender:)))
        navigationItem.leftBarButtonItem = newBackButton
        navigationItem.hidesBackButton = true

        content.nextButton.setTitle(NSLocalizedString("id_next", comment: ""), for: .normal)
        content.nextButton.setGradient(true)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(true)
        content.nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        pageCounter = 0
        loadWords()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.nextButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let verifyMnemonics = segue.destination as? VerifyMnemonicsViewController {
            verifyMnemonics.mnemonic = mnemonic
        }
    }

    @objc func click(_ sender: UIButton) {
        if pageCounter == 3 {
            self.performSegue(withIdentifier: "next", sender: nil)
        } else {
            pageCounter += 1
            loadWords()
        }
    }

    @objc func back(sender: UIBarButtonItem) {
        if pageCounter == 0 {
            navigationController?.popViewController(animated: true)
        } else {
            pageCounter -= 1
            loadWords()
        }
    }

    func loadWords() {
        content.progressView.progress = Float(pageCounter + 1) / 4
        let start = pageCounter * 6
        let end = start + 6
        for index in start..<end {
            let real = index+1
            let formattedString = NSMutableAttributedString(string: String("\(real) \(mnemonic[index])"))
            formattedString.setColor(color: UIColor.customMatrixGreen(), forText: String(format: "%d", real))
            formattedString.setFont(font: UIFont.systemFont(ofSize: 13), stringValue: String(format: "%d", real))
            content.arrayLabels[index % 6].attributedText = formattedString
        }
    }
}

@IBDesignable
class CreateWalletView: UIView {
    @IBOutlet weak var progressView: UIProgressView!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var word1: UILabel!
    @IBOutlet weak var word2: UILabel!
    @IBOutlet weak var word3: UILabel!
    @IBOutlet weak var word4: UILabel!
    @IBOutlet weak var word5: UILabel!
    @IBOutlet weak var word6: UILabel!

    lazy var arrayLabels: [UILabel] = [self.word1, self.word2, self.word3, self.word4, self.word5, self.word6]

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        nextButton.updateGradientLayerFrame()
    }
}
