//
//  AlwaysAskPassphrase.swift
//  gaios
//
//  Created by Mauro Olivo on 28/07/22.
//  Copyright © 2022 Blockstream Corporation. All rights reserved.
//

import Foundation

class AlwaysAskPassphraseHelper {

    static func isInList(_ accountId: String?) -> Bool {

        guard let accountId = accountId else { return false }
        if let list = UserDefaults.standard.object(forKey: AppStorage.alwaysAskPassphrase) as? [String] {
            print("alwaysAskPassphrase \(list)")
            return list.filter { $0 == accountId }.count != 0
        } else {
            return false
        }
    }

    static func add(_ accountId: String?) {
        guard let accountId = accountId else { return }
        if let list = UserDefaults.standard.object(forKey: AppStorage.alwaysAskPassphrase) as? [String] {

            if list.filter { $0 == accountId }.count == 0 {
                let updatedList = list + [accountId]
                UserDefaults.standard.setValue(updatedList, forKey: AppStorage.alwaysAskPassphrase)
            }
        } else {
            UserDefaults.standard.setValue([accountId], forKey: AppStorage.alwaysAskPassphrase)
        }
    }

    static func remove(_ accountId: String?) {
        guard let accountId = accountId else { return }
        if let list = UserDefaults.standard.object(forKey: AppStorage.alwaysAskPassphrase) as? [String] {

            let purgedList: [String] = list.filter { $0 != accountId }
            UserDefaults.standard.setValue(purgedList, forKey: AppStorage.alwaysAskPassphrase)
        } else {
            // nothing to do
        }
    }
}
