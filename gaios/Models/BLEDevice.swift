import Foundation
import AsyncBluetooth
import gdk
import hw

class BLEDevice {
    let peripheral: Peripheral
    let device: HWDevice
    let interface: HWProtocol
    init(peripheral: Peripheral, device: HWDevice, interface: HWProtocol) {
        self.peripheral = peripheral
        self.device = device
        self.interface = interface
    }
}
