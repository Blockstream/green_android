import XCTest

class SendBtcConfirmation: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendBtcConfirmationScreen.view]
    }
    
    @discardableResult
    func drag() -> Self {
        
        let e = app.otherElements[AccessibilityIdentifiers.SendBtcConfirmationScreen.slidingBtn]
        
        print(e.frame)
        let start = e.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        let finish = e.coordinate(withNormalizedOffset: CGVector(dx: 10, dy: 0))
        start.press(forDuration: 0.5, thenDragTo: finish)
        
        return self
    }
}
