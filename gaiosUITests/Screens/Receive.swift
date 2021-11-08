import XCTest

class Receive: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ReceiveScreen.view]
    }
    
    
    @discardableResult
    func tapQrCode() -> Self {
        tap(button: AccessibilityIdentifiers.ReceiveScreen.qrCodeBtn)
        return self
    }
    
    @discardableResult
    func tapBack() -> Self {
        _ = app.navigationBars.buttons.element(boundBy: 0).waitForExistence(timeout: 3)
        app.navigationBars.buttons.element(boundBy: 0).tap()
        return self
    }
    
}
