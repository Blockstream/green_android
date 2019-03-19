import Foundation
import UIKit

class ShowMnemonicsViewController: UIViewController {
    var viewArray: Array<UIView> = []

    override func viewDidLoad() {
        super.viewDidLoad()
        createViews()
        title = NSLocalizedString("id_mnemonic", comment: "")
    }

    func createViews() {
        let screenSize: CGRect = UIScreen.main.bounds
        let viewWidth = (screenSize.width - 40) / 4
        let viewHeight = viewWidth / 1.61

        let res = try? getSession().getMnemmonicPassphrase(password: "")
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
            NSLayoutConstraint(item: label, attribute: NSLayoutAttribute.centerX, relatedBy: NSLayoutRelation.equal, toItem: myView, attribute: NSLayoutAttribute.centerX, multiplier: 1, constant: 0).isActive = true
            NSLayoutConstraint(item: label, attribute: NSLayoutAttribute.top, relatedBy: NSLayoutRelation.equal, toItem: myView, attribute: NSLayoutAttribute.top, multiplier: 1, constant: 4).isActive = true
            NSLayoutConstraint(item: label, attribute: NSLayoutAttribute.width, relatedBy: NSLayoutRelation.equal, toItem: myView, attribute: NSLayoutAttribute.width, multiplier: 1, constant: 0).isActive = true

            //mnemonic label
            let mnemonicLabel = UILabel(frame: CGRect(x: 0, y: 0, width: 200, height: 21))
            mnemonicLabel.textAlignment = .center
            mnemonicLabel.text = String(mnemonic[index])
            mnemonicLabel.font = UIFont.systemFont(ofSize: 16)
            mnemonicLabel.textColor = UIColor.customTitaniumLight()
            mnemonicLabel.translatesAutoresizingMaskIntoConstraints = false
            myView.addSubview(mnemonicLabel)
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutAttribute.centerX, relatedBy: NSLayoutRelation.equal, toItem: myView, attribute: NSLayoutAttribute.centerX, multiplier: 1, constant: 0).isActive = true
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutAttribute.centerY, relatedBy: NSLayoutRelation.equal, toItem: myView, attribute: NSLayoutAttribute.centerY, multiplier: 1, constant: 4).isActive = true
            NSLayoutConstraint(item: mnemonicLabel, attribute: NSLayoutAttribute.width, relatedBy: NSLayoutRelation.equal, toItem: myView, attribute: NSLayoutAttribute.width, multiplier: 1, constant: 0).isActive = true

            self.view.addSubview(myView)
            //left constraint
            NSLayoutConstraint(item: myView, attribute: NSLayoutAttribute.width, relatedBy: NSLayoutRelation.equal, toItem: nil, attribute: NSLayoutAttribute.width, multiplier: 1, constant: viewWidth).isActive = true
            NSLayoutConstraint(item: myView, attribute: NSLayoutAttribute.height, relatedBy: NSLayoutRelation.equal, toItem: nil, attribute: NSLayoutAttribute.height, multiplier: 1, constant: viewHeight).isActive = true
            if(index == 0 || index % 4 == 0) {
                NSLayoutConstraint(item: myView, attribute: NSLayoutAttribute.leading, relatedBy: NSLayoutRelation.equal, toItem: view, attribute: NSLayoutAttribute.leading, multiplier: 1, constant: 20).isActive = true
            } else {
                NSLayoutConstraint(item: myView, attribute: NSLayoutAttribute.leading, relatedBy: NSLayoutRelation.equal, toItem: viewArray[index - 1], attribute: NSLayoutAttribute.trailing, multiplier: 1, constant: -1).isActive = true
            }
            //top constraint
            if (index < 4) {
                NSLayoutConstraint(item: myView, attribute: NSLayoutAttribute.top, relatedBy: NSLayoutRelation.equal, toItem: self.view, attribute: NSLayoutAttribute.top, multiplier: 1, constant: 90).isActive = true
            } else {
                NSLayoutConstraint(item: myView, attribute: NSLayoutAttribute.top, relatedBy: NSLayoutRelation.equal, toItem: viewArray[index - 4], attribute: NSLayoutAttribute.bottom, multiplier: 1, constant: -1).isActive = true
            }
        }
    }
}
