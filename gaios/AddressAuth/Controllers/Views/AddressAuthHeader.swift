import UIKit

class AddressAuthHeader: UIView {

    @IBOutlet weak var lblAddress: UILabel!
    @IBOutlet weak var lblTx: UILabel!
    @IBOutlet weak var lblActions: UILabel!

    func configure() {
        [lblAddress, lblTx, lblActions].forEach{
            $0?.setStyle(.sectionTitle)
        }
        lblAddress.text = "id_address".localized
        lblTx.text = "Tx"
        lblActions.text = "id_actions".localized
    }

}
