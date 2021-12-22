import Foundation
import UIKit

class ShowMnemonicsViewController: UIViewController {
    var viewArray: [UIView] = []
    var viewWidth: CGFloat {
        get { return ((UIScreen.main.bounds.width / 3) - 10) }
    }
    var viewHeight: CGFloat {
        get { return viewWidth / 1.75 }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        createViews()
        title = NSLocalizedString("id_recovery_phrase", comment: "")
    }

    func createViews() {
        let res = try? SessionsManager.current.getMnemonicPassphrase(password: "")
        guard let mnemonic = res?.split(separator: " ") else { return }
        for index in 0..<mnemonic.count {
            let myView = UIView(frame: CGRect(x: 0, y: 0, width: 100, height: viewHeight))
            myView.translatesAutoresizingMaskIntoConstraints = false
            myView.borderWidth = 5
            myView.borderColor = UIColor.customTitaniumDark()
            myView.backgroundColor = UIColor.customMnemonicDark()
            viewArray.append(myView)
            //index label
            let label = UILabel(frame: CGRect(x: 0, y: 0, width: 200, height: 21))
            label.textAlignment = .center
            label.text = String(index + 1)
            label.font = UIFont.systemFont(ofSize: 12)
            label.textColor = UIColor.customMatrixGreen()
            label.translatesAutoresizingMaskIntoConstraints = false
            myView.addSubview(label)
            NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.centerX, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.centerX, multiplier: 1, constant: 0).isActive = true
            NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.top, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.top, multiplier: 1, constant: 10).isActive = true
            NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: 0).isActive = true

            //mnemonic label
            let mnemonicLabel = UILabel(frame: CGRect(x: 0, y: 0, width: 200, height: 21))
            mnemonicLabel.textAlignment = .center
            mnemonicLabel.text = String(mnemonic[index])
            mnemonicLabel.font = UIFont.systemFont(ofSize: 14)
            mnemonicLabel.textColor = UIColor.white
            mnemonicLabel.adjustsFontSizeToFitWidth = true
            mnemonicLabel.minimumScaleFactor = 0.5
            mnemonicLabel.translatesAutoresizingMaskIntoConstraints = false
            myView.addSubview(mnemonicLabel)
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutConstraint.Attribute.centerX, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.centerX, multiplier: 1, constant: 0).isActive = true
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutConstraint.Attribute.centerY, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.centerY, multiplier: 1, constant: 3).isActive = true
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: 0).isActive = true

            self.view.addSubview(myView)
            //left constraint
            NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: viewWidth).isActive = true
            NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.height, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.height, multiplier: 1, constant: viewHeight).isActive = true
            if index == 0 || index % 3 == 0 {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.leading, relatedBy: NSLayoutConstraint.Relation.equal, toItem: view, attribute: NSLayoutConstraint.Attribute.leading, multiplier: 1, constant: 20).isActive = true
            } else {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.leading, relatedBy: NSLayoutConstraint.Relation.equal, toItem: viewArray[index - 1], attribute: NSLayoutConstraint.Attribute.trailing, multiplier: 1, constant: -1).isActive = true
            }
            //top constraint
            if index < 3 {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.top, relatedBy: NSLayoutConstraint.Relation.equal, toItem: self.view, attribute: NSLayoutConstraint.Attribute.top, multiplier: 1, constant: 10).isActive = true
            } else {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.top, relatedBy: NSLayoutConstraint.Relation.equal, toItem: viewArray[index - 3], attribute: NSLayoutConstraint.Attribute.bottom, multiplier: 1, constant: -1).isActive = true
            }
        }
    }
}
