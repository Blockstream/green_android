//
//  CheckButton.swift
//  gaios
//
//  Created by Mauro Olivo on 03/03/21.
//  Copyright © 2021 Blockstream Corporation. All rights reserved.
//

import UIKit

final class CheckButton: UIButton {

    private let tapGesture = UITapGestureRecognizer()
    /// :nodoc:
    override func awakeFromNib() {
        super.awakeFromNib()

        setupUI()
    }

    deinit {
        removeGestureRecognizer(tapGesture)
    }

    /// :nodoc:
    override func draw(_ rect: CGRect) {
        super.draw(rect)

        setupUI()
    }

    /// Performs the first setup of the button.
    private func setupUI() {

        setTitle(nil, for: [.normal, .disabled, .selected])
        setBackgroundImage(UIImage(), for: .normal)
        setBackgroundImage(UIImage(named: "check"), for: .selected)
        layer.borderWidth = 1.0
        layer.borderColor = UIColor.customGrayLight().cgColor
        layer.cornerRadius = 3.0

        tapGesture.addTarget(self, action: #selector(didTap))
        addGestureRecognizer(tapGesture)
    }

    @objc private func didTap() {
      isSelected.toggle()
      sendActions(for: .touchUpInside)
    }
}
