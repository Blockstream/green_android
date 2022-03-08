import XCTest

class Send: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendScreen.view]
    }

    @discardableResult
    func pasteAddress() -> Self {
        tap(button: AccessibilityIdentifiers.SendScreen.pasteAddressBtn)
        return self
    }

    @discardableResult
    func typeAmount(_ txt: String) -> Self {
        let e = app.textFields[AccessibilityIdentifiers.SendScreen.amountField]
        return type(value: txt, in: e)
    }

    @discardableResult
    func tapDone() -> Self {
        tap(button: AccessibilityIdentifiers.KeyboardView.done)
        return self
    }
    
    @discardableResult
    func tapNext() -> Self {
        
        let btn = app.buttons[AccessibilityIdentifiers.SendScreen.nextBtn]
        var numberTry = 0
        while numberTry < 20 {
            if btn.isHittable {
                break
            } else {
                sleep(1)
                numberTry += 1
            }
        }
        tap(button: AccessibilityIdentifiers.SendScreen.nextBtn)
        return self
    }
    
    @discardableResult
    func tapCustomFee() -> Self {
        var numberTry = 0
        while numberTry < 5 {
            sleep(1)
            numberTry += 1
        }
        tap(button: AccessibilityIdentifiers.SendScreen.setCutomFeeBtn)
        return self
    }
    
    @discardableResult
    func chooseAsset() -> Self {
        tap(button: AccessibilityIdentifiers.SendScreen.chooseAssetBtn)
        return self
    }
    
    @discardableResult
    func tapSendAll() -> Self {
        var numberTry = 0
        while numberTry < 5 {
            sleep(1)
            numberTry += 1
        }
        tap(button: AccessibilityIdentifiers.SendScreen.sendAllBtn)
        return self
    }
    
    @discardableResult
    func getAmount() -> String {
        let e = app.textFields[AccessibilityIdentifiers.SendScreen.amountField]
        return e.value as! String
    }
}
