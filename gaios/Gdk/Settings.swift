import Foundation
import gdk

extension Settings {

    func getScreenLock() -> ScreenLockType {
        let account = AccountsRepository.shared.current
        if account?.hasBioPin ?? false && account?.hasManualPin ?? false {
            return .All
        } else if account?.hasBioPin ?? false {
            let biometryType = AuthenticationTypeHandler.biometryType
            return biometryType == .faceID ? .FaceID : .TouchID
        } else if account?.hasManualPin ?? false {
            return .Pin
        } else {
            return .None
        }
    }
}
