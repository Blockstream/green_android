import UIKit

class HWWConnectViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblStateHint: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var successCircle: UIView!
    @IBOutlet weak var failureCircle: UIView!
    @IBOutlet weak var faailureImage: UIImageView!
    @IBOutlet weak var btnTryAgain: UIButton!
    @IBOutlet weak var deviceImage: UIImageView!
    @IBOutlet weak var arrowImage: UIImageView!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var tableViewHeight: NSLayoutConstraint!
    @IBOutlet weak var deviceImageAlign: NSLayoutConstraint!

    var hwwType: SupportedHW!

    var networks: [AvailableNetworks] = AvailableNetworks.allCases
    var cellH = 70.0

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    var hwwState: HWWState! {
        didSet {
            self.updateState()
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()

        hwwState = .connecting

        simul()
    }

    func simul() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            self.hwwState = .connected
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 6.0) {
            self.hwwState = .connecting
            self.showLoader()
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 9.0) {
            self.hwwState = .connectFailed
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 12.0) {
            self.hwwState = .selectNetwork
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 18.0) {
            self.hwwState = .followDevice
        }
    }

    func setContent() {
        lblTitle.text = hwwType.name()
        btnTryAgain.setTitle("Try Again", for: .normal)
    }

    func setStyle() {
        successCircle.borderWidth = 2.0
        successCircle.layer.cornerRadius = successCircle.frame.size.width / 2.0
        successCircle.borderColor = UIColor.customMatrixGreen()
        failureCircle.borderWidth = 2.0
        failureCircle.layer.cornerRadius = successCircle.frame.size.width / 2.0
        failureCircle.borderColor = UIColor.white
        btnTryAgain.cornerRadius = 4.0
        arrowImage.image = UIImage(named: "ic_hww_arrow")?.maskWithColor(color: UIColor.customMatrixGreen())
        faailureImage.image = UIImage(named: "cancel")?.maskWithColor(color: UIColor.white)
        tableViewHeight.constant = CGFloat(networks.count) * CGFloat(cellH)
        deviceImage.image = hwwType.deviceImage()
        deviceImageAlign.constant = hwwType.alignConstraint()
    }

    func updateState() {
        navigationItem.setHidesBackButton(true, animated: true)
        hideLoader()
        successCircle.isHidden = true
        failureCircle.isHidden = true
        btnTryAgain.isHidden = true
        deviceImage.isHidden = false
        arrowImage.isHidden = true
        tableView.isHidden = true

        switch hwwState {
        case .connecting:
            showLoader()
            lblStateHint.text = "Connecting to your device"
        case .connected:
            lblStateHint.text = "Succefully connected"
            successCircle.isHidden = false
        case .connectFailed:
            navigationItem.setHidesBackButton(false, animated: true)
            lblStateHint.text = "Connection failed."
            failureCircle.isHidden = false
            btnTryAgain.isHidden = false
        case .selectNetwork:
            lblStateHint.text = "Select a network."
            deviceImage.isHidden = true
            tableView.isHidden = false
        case .followDevice:
            lblStateHint.text = "Follow the instructions on your device."
            navigationItem.setHidesBackButton(false, animated: true)
            arrowImage.isHidden = false
        case .none:
            break
        }
    }

    @IBAction func btnTryAgain(_ sender: Any) {
        print("try again")
    }

}

extension HWWConnectViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return networks.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if let cell = tableView.dequeueReusableCell(withIdentifier: "HWWNetworkCell") as? HWWNetworkCell {
            cell.configure(networks[indexPath.row])
            cell.selectionStyle = .none
            return cell
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return CGFloat(cellH)
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        print(indexPath)
    }
}

extension HWWConnectViewController {
    func showLoader() {
        self.view.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerXAnchor),
            loadingIndicator.centerYAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerYAnchor),
            loadingIndicator.widthAnchor
                .constraint(equalToConstant: self.loaderPlaceholder.frame.width),
            loadingIndicator.heightAnchor
                .constraint(equalTo: self.loadingIndicator.widthAnchor)
        ])

        loadingIndicator.isAnimating = true
    }

    func hideLoader() {
        loadingIndicator.isAnimating = false
    }
}
