import Foundation

public class GDKSession: Session {
    public var ephemeral = false
    public var netParams = [String: Any]()

    public override func connect(netParams: [String: Any]) throws {
        try super.connect(netParams: netParams)
        self.netParams = netParams
    }

    public override init() {
        try! super.init()
    }

    deinit {
        super.setNotificationHandler(notificationCompletionHandler: nil)
    }

    public func loginUserSW(details: [String: Any]) throws -> TwoFactorCall {
        try loginUser(details: details)
    }

    public func loginUserHW(device: [String: Any]) throws -> TwoFactorCall {
        try loginUser(details: [:], hw_device: ["device": device])
    }

    public func registerUserSW(details: [String: Any]) throws -> TwoFactorCall {
        try registerUser(details: details)
    }

    public func registerUserHW(device: [String: Any]) throws -> TwoFactorCall {
        try registerUser(details: [:], hw_device: ["device": device])
    }
    
    public override func setNotificationHandler(notificationCompletionHandler: NotificationCompletionHandler?) {
        super.setNotificationHandler(notificationCompletionHandler: notificationCompletionHandler)
    }

}
