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
    
    @Published private(set) var isScanning = false
    @Published private(set) var peripherals: [ScanListItem] = []
    @Published private(set) var error: String?
    @Published private(set) var isConnected = false

    let centralManager: CentralManager
    private static var cachedPeripherals = [ScanListItem]()
    
    init(centralManager: CentralManager = CentralManager.shared) {
        self.centralManager = centralManager
    }
    
    func startScan(deviceType: DeviceType) {
        self.error = nil
        self.isScanning = true
        peripherals = ScanViewModel.cachedPeripherals
        Task {
            do {
                try await self.scan(deviceType: deviceType)
            } catch {
                DispatchQueue.main.async {
                    self.error = error.localizedDescription
                    self.isScanning = false
                }
            }
        }
    }
    
    func scan(deviceType: DeviceType) async throws {
        if self.isScanning == false { return }
        try await self.centralManager.waitUntilReady()
        let service = deviceType == .Jade ? BleJade.SERVICE_UUID : BleLedger.SERVICE_UUID
        let scanDataStream = try await centralManager.scanForPeripherals(withServices: [CBUUID(string: service.uuidString)])
        for await scanData in scanDataStream {
            addPeripheral(scanData.peripheral, for: deviceType)
        }
    }
    func addPeripheral(_ peripheral: Peripheral, for deviceType: DeviceType) {
        let identifier = peripheral.identifier
        let name = peripheral.name ?? ""
        let peripheral = ScanListItem(identifier: identifier, name: name, type: deviceType)
        if peripheral.type == deviceType {
            DispatchQueue.main.async {
                if self.peripherals.contains(where: { $0.identifier == identifier || $0.name == name }) {
                    self.peripherals.removeAll(where: { $0.identifier == identifier || $0.name == name })
                    ScanViewModel.cachedPeripherals.removeAll(where: { $0.identifier == identifier || $0.name == name })
                }
                self.peripherals.append(peripheral)
                ScanViewModel.cachedPeripherals.append(peripheral)
            }
        }
    }
    
    func stopScan() async {
        isScanning = false
        if centralManager.isScanning {
            await centralManager.stopScan()
        }
    }
    
    func peripheral(_ peripheralID: UUID) -> Peripheral? {
        centralManager.retrievePeripherals(withIdentifiers: [peripheralID]).first
    }
    
    func connect(_ peripheralID: UUID) {
        guard let peripheral = self.peripheral(peripheralID) else {
            self.error = "Unknown peripheral. Did you forget to scan?"
            return
        }
        Task {
            do {
                if self.centralManager.isScanning {
                    await self.centralManager.stopScan()
                }
                try await self.centralManager.connect(peripheral)
                DispatchQueue.main.async {
                    self.isConnected = true
                }
                
            } catch {
                DispatchQueue.main.async {
                    self.error = error.localizedDescription
                }
            }
        }
    }
    
    func cancel(_ peripheralID: UUID) {
        guard let peripheral = self.peripheral(peripheralID) else {
            self.error = "Unknown peripheral. Did you forget to scan?"
            return
        }
        Task {
            do {
                try await self.centralManager.connect(peripheral)
            } catch {}
        }
    }
}
