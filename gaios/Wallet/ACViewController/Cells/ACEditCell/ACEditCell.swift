import UIKit

class ACEditCell: UITableViewCell {

    var isFiat = false

    @IBOutlet weak var btnAdd: UIButton!
    @IBOutlet weak var btnArchive: UIButton!

    class var identifier: String { return String(describing: self) }

    var onAdd: (() -> Void)?
    var onArchive: (() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
        btnAdd.setTitle("Add New Account", for: .normal)
        btnArchive.setTitle("No Archived Accounts (0)", for: .normal)
    }

    var viewModel: ACEditCellModel? {
        didSet { }
    }

    func configure(onAdd: (() -> Void)?,
                   onArchive: (() -> Void)?
    ) {
        self.onAdd = onAdd
        self.onArchive = onArchive

        btnArchive.alpha = 0.4
    }

    @IBAction func btnAdd(_ sender: Any) {
        onAdd?()
    }

    @IBAction func btnArchive(_ sender: Any) {
        onArchive?()
    }
}
