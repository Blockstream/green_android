import XCTest

class Home: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.HomeScreen.view]
    }

//    @discardableResult
//    func tapAddWalletView() -> Self {
//        tap(app.otherElements[AccessibilityIdentifiers.HomeScreen.addWalletView])
//        return self
//    }
    
    @discardableResult
    func tapAddWalletView(connectionTimeout: TimeInterval = 25) -> Self {

        let lastCell = rootElement.tables.firstMatch.cells.allElementsBoundByIndex.last
        
        let MAX_SCROLLS = 10
        var count = 0
        while lastCell!.isHittable == false && count < MAX_SCROLLS {
            rootElement.tables.firstMatch.swipeUp()
            count += 1
        }
        tap(app.otherElements[AccessibilityIdentifiers.HomeScreen.addWalletView])
        return self
    }
}
