//
//  DialogListCell.swift
//  gaios
//
//  Created by Mauro Olivo on 15/11/22.
//  Copyright © 2022 Blockstream Corporation. All rights reserved.
//

import UIKit

class DialogListCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(_ model: DialogListCellModel) {
        icon.image = model.icon
        lblTitle.text = model.title
    }
}
