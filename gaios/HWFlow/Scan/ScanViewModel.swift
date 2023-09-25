import Foundation
import AsyncBluetooth
import Combine
import hw
import CoreBluetooth

enum DeviceType: Int {
    case Jade
    case Ledger
}

class ScanViewModel: ObservableObject {

    @Published private(set) var peripherals: [ScanListItem] = []
    @Published private(set) var error: String?
    @Published private(set) var isConnected = false
    let centralManager: CentralManager

    init(centralManager: CentralManager = CentralManager.shared) {
        self.centralManager = centralManager
    }

    func scan(deviceType: DeviceType) async throws {
        if centralManager.isScanning {
            return
        }
        reset()
        try await self.centralManager.waitUntilReady()
        let uuid = deviceType == .Jade ? BleJade.SERVICE_UUID : BleLedger.SERVICE_UUID
        let service = CBUUID(string: uuid.uuidString)
        let connectedPeripherals = centralManager.retrieveConnectedPeripherals(withServices: [service])
        connectedPeripherals.forEach { addPeripheral($0, for: deviceType) }
        let scanDataStream = try await centralManager.scanForPeripherals(withServices: [service])
        for await scanData in scanDataStream {
            DispatchQueue.main.async {
                self.addPeripheral(scanData.peripheral, for: deviceType)
            }
        }
    }

    func addPeripheral(_ peripheral: Peripheral, for deviceType: DeviceType) {
        let identifier = peripheral.identifier
        let name = peripheral.name ?? ""
        let peripheral = ScanListItem(identifier: identifier, name: name, type: deviceType)
        if peripheral.type == deviceType {
            if peripherals.contains(where: { $0.identifier == identifier || $0.name == name }) {
                peripherals.removeAll(where: { $0.identifier == identifier || $0.name == name })
            }
            peripherals.append(peripheral)
        }
    }
    
    func reset() {
        DispatchQueue.main.async {
            self.peripherals.removeAll()
        }
    }

    func stopScan() async {
        if centralManager.isScanning {
            await centralManager.stopScan()
        }
    }

    func peripheral(_ peripheralID: UUID) -> Peripheral? {
        centralManager.retrievePeripherals(withIdentifiers: [peripheralID]).first
    }
    
    func connect(_ peripheralID: UUID) async throws {
        if let peripheral = peripheral(peripheralID) {
            try await centralManager.connect(peripheral)
        }
    }
}
