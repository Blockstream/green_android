import UIKit

class HWFlowBaseViewController: UIViewController {

    let mash = UIImageView(image: UIImage(named: "il_mash")!)

    override func viewDidLoad() {
        super.viewDidLoad()

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

    override func viewWillAppear(_ animated: Bool) {
        UIApplication.shared.isIdleTimerDisabled = true
    }

    override func viewWillDisappear(_ animated: Bool) {
        UIApplication.shared.isIdleTimerDisabled = false
    }

    @MainActor
    func onError(_ err: Error) {
        stopLoader()
        let txt = BleViewModel.shared.toBleError(err, network: nil).localizedDescription
        DropAlert().error(message: txt.localized)
    }
}
