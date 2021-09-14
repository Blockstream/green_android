//
//  OverviewAccountCell.swift
//  gaios
//
//  Created by Mauro Olivo on 07/09/21.
//  Copyright © 2021 Blockstream Corporation. All rights reserved.
//

import UIKit

class OverviewAccountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var bgShadow: UIView!
    @IBOutlet weak var bgHint: UIView!
    @IBOutlet weak var lblAccountTitle: UILabel!
    @IBOutlet weak var lblAccountHint: UILabel!
    @IBOutlet weak var actionBtn: UIButton!
    var action: VoidToVoid?

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.layer.cornerRadius = 6.0
        bgShadow.cornerRadius = 6.0
        bgShadow.alpha = 0.5
        bgHint.layer.cornerRadius = 3.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblAccountTitle.text = ""
        lblAccountHint.text = ""
    }

    func configure(account: WalletItem, action: VoidToVoid? = nil, color: UIColor, showAccounts: Bool) {
        bg.backgroundColor = color
        if showAccounts { bgShadow.backgroundColor = .clear } else { bgShadow.backgroundColor = color }
        self.lblAccountTitle.text = account.localizedName()
        let accountType: AccountType? = AccountType(rawValue: account.type)
        self.lblAccountHint.text = accountType?.name ?? ""
        self.action = action
        self.actionBtn.isHidden = action == nil
    }

    @IBAction func actionBtn(_ sender: Any) {
        self.action?()
    }
}
