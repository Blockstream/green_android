import XCTest

class Receive: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ReceiveScreen.view]
    }
    
    
    @discardableResult
    func tapQrCode() -> Self {

        let btn = app.buttons[AccessibilityIdentifiers.ReceiveScreen.addressBtn]
        var numberTry = 0
        while numberTry < 20 {
            if btn.label.count > 1 {
                break
            } else {
                sleep(1)
                numberTry += 1
            }
        }
        
        tap(button: AccessibilityIdentifiers.ReceiveScreen.addressBtn)
        return self
    }
    
    @discardableResult
    func tapBack() -> Self {
        _ = app.navigationBars.buttons.element(boundBy: 0).waitForExistence(timeout: 3)
        app.navigationBars.buttons.element(boundBy: 0).tap()
        return self
    }
    
    @discardableResult
    func tapMoreOptions() -> Self {
        tap(button: AccessibilityIdentifiers.ReceiveScreen.moreOptionsBtn)
        return self
    }
    
}
