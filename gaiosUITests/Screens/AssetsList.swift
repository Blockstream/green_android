import XCTest

class AssetsList: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.AssetsListScreen.view]
    }

    
    @discardableResult
    func selectLBtc() -> Self {
        app.tables[AccessibilityIdentifiers.AssetsListScreen.table]
            .cells.element(boundBy: 0).tap()
        return self
    }
}

