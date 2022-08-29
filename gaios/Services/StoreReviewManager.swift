import Foundation
import StoreKit

class StoreReviewHelper {

    static let shared = StoreReviewHelper()

    var months = 4

    var appReview: Int?

    private func isReviewDateValid() -> Bool {

        let now = Date()

        if let storeReviewDate = UserDefaults.standard.object(forKey: AppStorage.storeReviewDate) as? Date {

            if let appReview = appReview, appReview > months {
                months = appReview
            }

            if now - storeReviewDate > Double( months * 30 * 86400 ) {
                UserDefaults.standard.set(now, forKey: AppStorage.storeReviewDate)
                return true
            } else {
                print("SKIP")
                return false
            }
        } else {
            UserDefaults.standard.set(now, forKey: AppStorage.storeReviewDate)
            return true
        }
    }

    private func requestReview() {

        SKStoreReviewController.requestReview()
    }
}

extension StoreReviewHelper {
    func request(isSendAll: Bool) {

        appReview = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigAppReview) as? Int

        if isSendAll { return }
        if appReview == 0 { return }
        if AnalyticsManager.shared.exceptionCounter != 0 { return }

        if !isReviewDateValid() { return }
        requestReview()
    }
}
