//
//  WalletCell.swift
//  gaios
//
//  Created by Mauro Olivo on 16/03/21.
//  Copyright © 2021 Blockstream Corporation. All rights reserved.
//

import UIKit

class WalletCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure(_ item: WalletListItem) {
        self.lblTitle.text = item.title
        self.icon.image = item.icon
    }

}
