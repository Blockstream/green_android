import XCTest

class Drawer: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DrawerMenuScreen.view]
    }
 
    @discardableResult
    func existsWallet(named name: String, connectionTimeout: TimeInterval = 25) -> Bool {
        let walletExistance = NSPredicate(format: "label MATCHES '\(name)'")
        let walletLabel = app.otherElements[AccessibilityIdentifiers.DrawerMenuScreen.view].tables
            .children(matching: .cell).staticTexts.element(matching: walletExistance)
        var existsWallet = walletLabel.waitForExistence(timeout: 3)

        var swipeUpTimes = 0
        while !existsWallet && swipeUpTimes < maxSwipeUp {
            rootElement.tables.cells.element(boundBy: 0).swipeUp()
            swipeUpTimes += 1

            existsWallet = walletLabel.exists
        }

        return existsWallet
    }
    
    @discardableResult
    func selectWallet(named name: String, connectionTimeout: TimeInterval = 25) -> Self {
        let walletExistance = NSPredicate(format: "label MATCHES '\(name)'")
        let walletLabel = app.otherElements[AccessibilityIdentifiers.DrawerMenuScreen.view].tables
            .children(matching: .cell).staticTexts.element(matching: walletExistance)
        var existsWallet = walletLabel.waitForExistence(timeout: 3)

        var swipeUpTimes = 0
        while !existsWallet && swipeUpTimes < maxSwipeUp {
            rootElement.tables.cells.element(boundBy: 0).swipeUp()
            swipeUpTimes += 1

            existsWallet = walletLabel.exists
        }

        return tap(walletLabel)
    }
    
    @discardableResult
    func tapAddWalletView(connectionTimeout: TimeInterval = 25) -> Self {
        
        let lastCell = rootElement.tables.firstMatch.cells.allElementsBoundByIndex.last
        let MAX_SCROLLS = 10
        var count = 0
        while lastCell!.isHittable == false && count < MAX_SCROLLS {
            rootElement.tables.firstMatch.swipeUp()
            count += 1
        }
        tap(app.otherElements[AccessibilityIdentifiers.DrawerMenuScreen.addWalletView])
        return self
    }
    
    @discardableResult
    func tapHWWBtn(_ name: String, connectionTimeout: TimeInterval = 25) -> Self {
        
        //"Ledger Nano X"
        
        let lastCell = rootElement.tables.firstMatch.cells.allElementsBoundByIndex.last
        let MAX_SCROLLS = 10
        var count = 0
        while lastCell!.isHittable == false && count < MAX_SCROLLS {
            rootElement.tables.firstMatch.swipeUp()
            count += 1
        }
        app.staticTexts[name].tap()

        return self
    }
    
}
