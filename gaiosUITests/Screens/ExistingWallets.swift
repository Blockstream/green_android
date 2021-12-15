import XCTest

class ExistingWallets: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ExistingWalletsScreen.view]
    }

    @discardableResult
    func checkWalletsExistance() -> Self {
        
        let exists = app.staticTexts["id_multisig_shield".localized(for: Self.self)].waitForExistence(timeout: 20)
                    
        if exists != true {
            XCTFail("checkWalletsExistance failure")
        }
        return self
    }
    
    @discardableResult
    func tapMultisig() -> Self {
        
        app.tables.element(boundBy: 0).cells.element(boundBy: 1).tap()
        return self
    }
    
    @discardableResult
    func tapManualRestore() -> Self {
        tap(button: AccessibilityIdentifiers.ExistingWalletsScreen.manualRestoreBtn)
        return self
    }
    
//    @discardableResult
//    func tapSave() -> Self {
//        tap(button: AccessibilityIdentifiers.WalletSettingsScreen.saveBtn)
//        return self
//    }
//    
//    @discardableResult
//    func tapCancel() -> Self {
//        tap(button: AccessibilityIdentifiers.WalletSettingsScreen.saveBtn)
//        return self
//    }
//    
//    @discardableResult
//    func isTorSetTo(_ value: Bool) -> Bool {
//        let switchTor = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.torSwitch]
//        var switchTotValue = false
//        if(switchTor.value as? String == "1") {
//            switchTotValue = true
//        }
//        return (value == switchTotValue)
//    }
//    
//    @discardableResult
//    func isTestnetSetTo(_ value: Bool) -> Bool {
//        let switchTor = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.testnetSwitch]
//        var switchTotValue = false
//        if(switchTor.value as? String == "1") {
//            switchTotValue = true
//        }
//        return (value == switchTotValue)
//    }
//    
//    @discardableResult
//    func tapTestnetSwitch() -> Self {
//        
//        let switchTestnet = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.testnetSwitch]
//        switchTestnet.tap()
//        
//        return self
//    }
}

