import UIKit
import RxBluetoothKit
import RxSwift

class HWFlowBaseViewController: UIViewController {

    var stateDisposable: Disposable?
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
        stateDisposable = BLEManager.shared.manager
                .observeState()
                .subscribe(onNext: { if $0 != .poweredOn { self.showBleState($0) }},
                           onError: { self.error($0) })
    }

    override func viewWillDisappear(_ animated: Bool) {
        stateDisposable?.dispose()
    }

    func showBleState(_ state: BluetoothState) {
        switch state {
        case .unknown:
            break
        case .unsupported:
            showError("Unsupported bluetooth".localized)
        case .unauthorized, .poweredOff, .resetting:
            showError("id_enable_bluetooth_from_system".localized)
        case .poweredOn:
            break
        }
    }
    func error(_ err: Error) {
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        showAlert(title: "id_error".localized, message: txt)
    }
}
