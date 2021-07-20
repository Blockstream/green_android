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
}
