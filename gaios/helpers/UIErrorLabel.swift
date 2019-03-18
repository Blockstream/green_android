import Foundation
import UIKit

class UIErrorLabel : UILabel {

    init() {
        super.init(frame: CGRect(x: 0, y: 0, width: 150, height: 21))
    }

    init(_ view: UIView) {
        super.init(frame: CGRect(x: 0, y: 0, width: 150, height: 21))
        setup(view)
    }

    func setup(_ view: UIView) {
        textAlignment = .center
        textColor = UIColor.errorRed()
        view.addSubview(self)
        translatesAutoresizingMaskIntoConstraints = false
        bottomAnchor.constraint(equalTo: view.layoutMarginsGuide.bottomAnchor, constant: -70).isActive = true
        leadingAnchor.constraint(equalTo: view.layoutMarginsGuide.leadingAnchor).isActive = true
        trailingAnchor.constraint(equalTo: view.layoutMarginsGuide.trailingAnchor).isActive = true
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
