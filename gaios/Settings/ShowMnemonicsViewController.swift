import Foundation
import UIKit

class ShowMnemonicsViewController: UIViewController {
    var viewArray: [UIView] = []
    var viewWidth: CGFloat {
        get { return (UIScreen.main.bounds.width - 40) / 4 }
    }
    var viewHeight: CGFloat {
        get { return viewWidth / 1.61 }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        createViews()
        title = NSLocalizedString("id_mnemonic", comment: "")
    }

    func createViews() {
        let res = try? getSession().getMnemonicPassphrase(password: "")
        guard let mnemonic = res?.split(separator: " ") else { return }
        for index in 0..<mnemonic.count {
            let myView = UIView(frame: CGRect(x: 0, y: 0, width: viewWidth, height: viewHeight))
            myView.translatesAutoresizingMaskIntoConstraints = false
            myView.borderWidth = 1
            myView.borderColor = UIColor.customTitaniumLight()
            viewArray.append(myView)
            //index label
            let label = UILabel(frame: CGRect(x: 0, y: 0, width: 200, height: 21))
            label.textAlignment = .center
            label.text = String(index + 1)
            label.font = UIFont.systemFont(ofSize: 12)
            label.textColor = UIColor.customTitaniumLight()
            label.translatesAutoresizingMaskIntoConstraints = false
            myView.addSubview(label)
            NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.centerX, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.centerX, multiplier: 1, constant: 0).isActive = true
            NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.top, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.top, multiplier: 1, constant: 4).isActive = true
            NSLayoutConstraint(item: label, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: 0).isActive = true

            //mnemonic label
            let mnemonicLabel = UILabel(frame: CGRect(x: 0, y: 0, width: 200, height: 21))
            mnemonicLabel.textAlignment = .center
            mnemonicLabel.text = String(mnemonic[index])
            mnemonicLabel.font = UIFont.systemFont(ofSize: 16)
            mnemonicLabel.textColor = UIColor.customTitaniumLight()
            mnemonicLabel.translatesAutoresizingMaskIntoConstraints = false
            myView.addSubview(mnemonicLabel)
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutConstraint.Attribute.centerX, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.centerX, multiplier: 1, constant: 0).isActive = true
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutConstraint.Attribute.centerY, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.centerY, multiplier: 1, constant: 4).isActive = true
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: myView, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: 0).isActive = true

            self.view.addSubview(myView)
            //left constraint
            NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: viewWidth).isActive = true
            NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.height, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.height, multiplier: 1, constant: viewHeight).isActive = true
            if index == 0 || index % 4 == 0 {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.leading, relatedBy: NSLayoutConstraint.Relation.equal, toItem: view, attribute: NSLayoutConstraint.Attribute.leading, multiplier: 1, constant: 20).isActive = true
            } else {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.leading, relatedBy: NSLayoutConstraint.Relation.equal, toItem: viewArray[index - 1], attribute: NSLayoutConstraint.Attribute.trailing, multiplier: 1, constant: -1).isActive = true
            }
            //top constraint
            if index < 4 {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.top, relatedBy: NSLayoutConstraint.Relation.equal, toItem: self.view, attribute: NSLayoutConstraint.Attribute.top, multiplier: 1, constant: 90).isActive = true
            } else {
                NSLayoutConstraint(item: myView, attribute: NSLayoutConstraint.Attribute.top, relatedBy: NSLayoutConstraint.Relation.equal, toItem: viewArray[index - 4], attribute: NSLayoutConstraint.Attribute.bottom, multiplier: 1, constant: -1).isActive = true
            }
        }
    }
}
