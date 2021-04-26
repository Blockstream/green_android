import UIKit

class HWWScanViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var arrow: UIImageView!
    @IBOutlet weak var card: UIView!
    @IBOutlet weak var lblHead: UILabel!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var deviceImage: UIImageView!
    @IBOutlet weak var deviceImageAlign: NSLayoutConstraint!

    var hwwType: SupportedHW!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {
        lblTitle.text = hwwType.name()
        lblHint.text = "Follow the instructions on your device."
        lblHead.text = "Available devices"
    }

    func setStyle() {
        card.layer.cornerRadius = 5.0
        arrow.image = UIImage(named: "ic_hww_arrow")?.maskWithColor(color: UIColor.customMatrixGreen())
        deviceImage.image = hwwType.deviceImage()
        deviceImageAlign.constant = hwwType.alignConstraint()
    }

}

extension HWWScanViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 5
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if let cell = tableView.dequeueReusableCell(withIdentifier: "HWWCell") as? HWWCell {
            cell.configure("Wallet \(indexPath.row)", "Not connected".uppercased())
            cell.selectionStyle = .none
            return cell
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 48.0
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let storyboard = UIStoryboard(name: "HWW", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "HWWConnectViewController") as? HWWConnectViewController {
            vc.hwwType = hwwType
            navigationController?.pushViewController(vc, animated: true)
        }
    }
}
