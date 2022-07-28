import Foundation
import StoreKit

class StoreReviewHelper {

    static let shared = StoreReviewHelper()

    private var interval: Double {
        return 122 * 86400 // days * seconds
    }

    func request(isSendAll: Bool) {

        if isSendAll { return }

        if !isReviewDateValid() { return }

        requestReview()
    }

    private func isReviewDateValid() -> Bool {

        let now = Date()

        if let storeReviewDate = UserDefaults.standard.object(forKey: AppStorage.storeReviewDate) as? Date {

            if now - storeReviewDate > interval {
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
