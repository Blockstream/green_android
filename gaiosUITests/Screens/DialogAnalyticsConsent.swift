import XCTest

class DialogAnalyticsConsent: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogAnalyticsConsentScreen.view]
    }

    @discardableResult
    func tapDeny() -> Self {
        tap(button: AccessibilityIdentifiers.DialogAnalyticsConsentScreen.denyBtn)
        return self
    }
}
