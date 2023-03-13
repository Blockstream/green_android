import UIKit

class HWFlowBaseViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        let img = UIImage(named: "il_mash")!
        let mash = UIImageView(image: img)
        mash.alpha = 0.6
        view.insertSubview(mash, at: 0)
        mash.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            mash.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 0),
            mash.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: 0),
            mash.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: 0),
            mash.heightAnchor.constraint(equalToConstant: view.frame.height / 1.8)
        ])
    }
}
