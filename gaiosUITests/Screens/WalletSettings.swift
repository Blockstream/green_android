import XCTest

class WalletSettings: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.WalletSettingsScreen.view]
    }

    @discardableResult
    func enableTor() -> Self {
        
        let switchTor = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.torSwitch]
        var switchTotValue = false
        if(switchTor.value as? String == "1") {
            switchTotValue = true
        }
        
        XCTAssert(switchTotValue == false)
        switchTor.tap()
        
        return self
    }
    
    @discardableResult
    func tapTorSwitch() -> Self {
        
        let switchTor = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.torSwitch]
        switchTor.tap()
        
        return self
    }
    
    @discardableResult
    func tapSave() -> Self {
        tap(button: AccessibilityIdentifiers.WalletSettingsScreen.saveBtn)
        return self
    }
    
    @discardableResult
    func tapCancel() -> Self {
        tap(button: AccessibilityIdentifiers.WalletSettingsScreen.saveBtn)
        return self
    }
    
    @discardableResult
    func isTorSetTo(_ value: Bool) -> Bool {
        let switchTor = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.torSwitch]
        var switchTotValue = false
        if(switchTor.value as? String == "1") {
            switchTotValue = true
        }
        return (value == switchTotValue)
    }
    
    @discardableResult
    func isTestnetSetTo(_ value: Bool) -> Bool {
        let switchTor = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.testnetSwitch]
        var switchTotValue = false
        if(switchTor.value as? String == "1") {
            switchTotValue = true
        }
        return (value == switchTotValue)
    }
    
    @discardableResult
    func tapTestnetSwitch() -> Self {
        
        let switchTestnet = app.switches[AccessibilityIdentifiers.WalletSettingsScreen.testnetSwitch]
        switchTestnet.tap()
        
        return self
    }
}

