import Foundation
import BreezSDK

public struct AppGreenlightCredentials: Codable {

    let deviceKey: Data
    let deviceCert: Data

    public init(deviceKey: Data, deviceCert: Data) {
        self.deviceKey = deviceKey
        self.deviceCert = deviceCert
    }

    init(gc: GreenlightCredentials) {
        self.deviceKey = Data(gc.deviceKey)
        self.deviceCert = Data(gc.deviceCert)
    }
    
    var greenlightCredentials: GreenlightCredentials {
        GreenlightCredentials(deviceKey: [UInt8](deviceKey),
                              deviceCert: [UInt8](deviceCert))
    }
}
