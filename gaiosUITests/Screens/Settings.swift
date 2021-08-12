import XCTest

class Settings: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SettingsScreen.view]
    }
    
    @discardableResult
    func tapWatchOnly(connectionTimeout: TimeInterval = 25) -> Self {
        app.otherElements[AccessibilityIdentifiers.SettingsScreen.view].tables.element(boundBy: 0).cells.element(boundBy: 1).tap()
        return self
    }
    
    @discardableResult
    func typeUsername(_ txt: String) -> Self {
        let e = app.textFields[AccessibilityIdentifiers.SettingsScreen.usernameField]
        return type(value: txt, in: e)
    }
    
    @discardableResult
    func typePassword(_ txt: String) -> Self {
        let e = app.textFields[AccessibilityIdentifiers.SettingsScreen.passwordField]
        return type(value: txt, in: e)
    }
    
    @discardableResult
    func tapSave() -> Self {
        app.alerts.firstMatch.buttons.lastMatch.tap()
        return self
    }
    
    @discardableResult
    func tapLogOut(connectionTimeout: TimeInterval = 25) -> Self {
        
        let btnExistance = NSPredicate(format: "label MATCHES '\("id_log_out".localized(for: Self.self))'")
        let btnLabel = app.otherElements[AccessibilityIdentifiers.SettingsScreen.view].tables
            .children(matching: .cell).staticTexts.element(matching: btnExistance)
        _ = btnLabel.waitForExistence(timeout: 3)

        return tap(btnLabel)
    }
    
    @discardableResult
    func tapSetupPin(connectionTimeout: TimeInterval = 25) -> Self {
        app.otherElements[AccessibilityIdentifiers.SettingsScreen.view].tables.element(boundBy: 0).cells.element(boundBy: 5).tap()
        return self
    }
}
