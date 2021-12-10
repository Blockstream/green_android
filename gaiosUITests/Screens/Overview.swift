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

}
