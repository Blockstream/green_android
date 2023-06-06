import Foundation

public protocol HWConnectionProtocol {
    func open() async throws
    func exchange(_ data: Data) async throws -> Data
    func read() async throws -> Data?
    func write(_ data: Data) async throws
    func close() async throws
}
