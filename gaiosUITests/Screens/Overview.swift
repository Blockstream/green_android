import XCTest

class Overview: Screen {

    override var rootElement: XCUIElement {
        return app.tables[AccessibilityIdentifiers.OverviewScreen.view]
    }
    
    @discardableResult
    func tapSettings() -> Self {
        tap(button: AccessibilityIdentifiers.OverviewScreen.settingsBtn)
        return self
    }

    @discardableResult
    func tapReceive() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.OverviewScreen.receiveView])
        return self
    }

    @discardableResult
    func appTap() -> Self {
        app.tap() //required to dismiss System Dialog
        return self
    }

    @discardableResult
    func tapSend() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.OverviewScreen.sendView])
        return self
    }
    
    @discardableResult
    func tapDrawerBtn(_ name: String) -> Self {
        app.staticTexts[name].tap()
        return self
    }

    @discardableResult
    func waitIsReady() -> Self {
        
        let btn = app.buttons[AccessibilityIdentifiers.OverviewScreen.settingsBtn]
        var numberTry = 0
        while numberTry < 15 {
            if btn.isHittable {
                return self
            } else {
                sleep(1)
                numberTry += 1
            }
        }
        return self
    }
    
    @discardableResult
    func waitTransactionsLoad() -> Self {
        
        var numberTry = 0
        while numberTry < 30 {
            let exists = app.staticTexts["id_your_transactions_will_be_shown".localized(for: Self.self)].waitForExistence(timeout: 1)
            if exists == true {
                sleep(1)
                numberTry += 1
            } else {
                return self
            }
        }
        return self
    }
    
}
