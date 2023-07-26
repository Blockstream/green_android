//
//  AddressAuthHeader.swift
//  gaios
//
//  Created by Mauro Olivo on 19/07/23.
//  Copyright © 2023 Blockstream Corporation. All rights reserved.
//

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
