import XCTest

class ReceiveBtc: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ReceiveBtcScreen.view]
    }
    
    
    @discardableResult
    func tapQrCode() -> Self {
        tap(app.images[AccessibilityIdentifiers.ReceiveBtcScreen.qrCodeView])
        return self
    }
    
    @discardableResult
    func tapBack() -> Self {
        app.navigationBars.buttons.element(boundBy: 0).tap()
        return self
    }
    
}
