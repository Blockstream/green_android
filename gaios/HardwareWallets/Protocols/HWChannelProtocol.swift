import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth

protocol HWChannelProtocol {
    /**
     * Open the communication channel to the device
     * @throw LedgerException if a communication error occurs
     */
    func open(_ peripheral: Peripheral) -> Observable<Data>

    /**
     * Exchange an APDU with the device. This method is blocking until the answer is received or an exception is thrown
     * @param apdu APDU to send to the device
     * @return response to the APDU including the Status Word
     * @throw LedgerException if a communication error occurs
     */
    func exchange(_ data: Data) -> Observable<Data>

    /**
     * Close the commmunication to the device
     * @throw LedgerException if a communication error occurs (can be safely ignored)
     */
    func close() -> Observable<Data>

    /**
     * Check if the communication channel has already been opened
     */
    func isOpened() -> Bool
}
