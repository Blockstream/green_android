import XCTest

class Container: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ContainerScreen.view]
    }
    
}
