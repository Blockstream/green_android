import Foundation
import RxSwift
import RxBluetoothKit
import CommonCrypto

public struct Firmware: Codable {
    enum CodingKeys: String, CodingKey {
        case filename = "filename"
        case version = "version"
        case config = "config"
        case fwsize = "fwsize"
        case fromVersion = "from_version"
        case fromConfig = "from_config"
        case patchSize = "patch_size"
    }
    public let filename: String
    public let version: String
    public let config: String
    public let fwsize: Int
    public let fromVersion: String?
    public let fromConfig: String?
    public let patchSize: Int?

    public func upgradable(_ jadeVersion: String) -> Bool {
        return self.config == "ble" && self.version > jadeVersion &&
        self.fromConfig ?? "ble" == "ble" && self.fromVersion ?? jadeVersion == jadeVersion
    }

    public var isDelta: Bool {
        return patchSize == nil
    }
}

public struct FirmwareVersions: Codable {
    public let full: [Firmware]?
    public let delta: [Firmware]?
}

public struct FirmwareChannels: Codable {
    public let beta: FirmwareVersions?
    public let stable: FirmwareVersions?
    public let previous: FirmwareVersions?
}

public class JadeOTA: JadeChannel {

    public static let MIN_ALLOWED_FW_VERSION = "0.1.24"
    public static let FW_SERVER_HTTPS = "https://jadefw.blockstream.com"
    public static let FW_SERVER_ONION = "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion"
    public static let FW_JADE_PATH = "/bin/jade/"
    public static let FW_JADEDEV_PATH = "/bin/jadedev/"
    public static let FW_JADE1_1_PATH = "/bin/jade1.1/"
    public static let FW_JADE1_1DEV_PATH = "/bin/jade1.1dev/"
    public static let BOARD_TYPE_JADE = "JADE"
    public static let BOARD_TYPE_JADE_V1_1 = "JADE_V1.1"
    public static let FEATURE_SECURE_BOOT = "SB"

    // Check Jade fmw against minimum allowed firmware version
    public func isJadeFwValid(_ version: String) -> Bool {
        return JadeOTA.MIN_ALLOWED_FW_VERSION <= version
    }

    public func download(_ fwpath: String, base64: Bool = false) -> [String: Any]? {
        let params: [String: Any] = [
            "method": "GET",
            "accept": base64 ? "base64": "json",
            "urls": ["\(JadeOTA.FW_SERVER_HTTPS)\(fwpath)",
                     "\(JadeOTA.FW_SERVER_ONION)\(fwpath)"] ]
        return Jade.shared.gdkRequestDelegate?.httpRequest(params: params)
    }

    public func firmwarePath(_ verInfo: JadeVersionInfo) -> String? {
        if ![JadeOTA.BOARD_TYPE_JADE, JadeOTA.BOARD_TYPE_JADE_V1_1].contains(verInfo.boardType) {
            return nil
        }
        let isV1BoardType = verInfo.boardType == JadeOTA.BOARD_TYPE_JADE
        // Alas the first version of the jade fmw didn't have 'BoardType' - so we assume an early jade.
        if verInfo.jadeFeatures.contains(JadeOTA.FEATURE_SECURE_BOOT) {
            // Production Jade (Secure-Boot [and flash-encryption] enabled)
            return isV1BoardType ? JadeOTA.FW_JADE_PATH : JadeOTA.FW_JADE1_1_PATH
        } else {
            // Unsigned/development/testing Jade
            return isV1BoardType ? JadeOTA.FW_JADEDEV_PATH : JadeOTA.FW_JADE1_1DEV_PATH
        }
    }

    public func firmwareData(_ verInfo: JadeVersionInfo) throws -> Firmware {
        // Get relevant fmw path (or if hw not supported)
        guard let fwPath = firmwarePath(verInfo) else {
            throw HWError.Abort("Unsupported hardware")
        }
        guard let res = download("\(fwPath)index.json"),
              let body = res["body"] as? [String: Any],
              let json = try? JSONSerialization.data(withJSONObject: body, options: []),
              let channels = try? JSONDecoder().decode(FirmwareChannels.self, from: json) else {
            throw HWError.Abort("Failed to fetch firmware index")
        }
        #if DEBUG
        let images = [channels.beta?.delta, channels.beta?.full, channels.stable?.delta, channels.stable?.full]
        #else
        let images = [channels.stable?.delta, channels.stable?.full]
        #endif
        for image in images {
            if let fmw = image?.filter({ $0.upgradable(verInfo.jadeVersion) }).first {
                return fmw
            }
        }
        throw HWError.Abort("No newer firmware found")
    }

    public func getBinary(_ verInfo: JadeVersionInfo, _ fmw: Firmware) throws -> Data {
        guard let fwPath = firmwarePath(verInfo) else {
            throw HWError.Abort("Unsupported hardware")
        }
        if let res = download("\(fwPath)\(fmw.filename)", base64: true),
            let body = res["body"] as? String,
            let data = Data(base64Encoded: body) {
            return data
        }
        throw HWError.Abort("Error downloading firmware file")
    }

    public func sha256(_ data: Data) -> Data {
        let length = Int(CC_SHA256_DIGEST_LENGTH)
        var hash = [UInt8](repeating: 0, count: length)
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
        }
        return Data(hash)
    }
    
    public func updateFirmware(version: JadeVersionInfo, firmware: Firmware, binary: Data) -> Observable<Bool> {
        let hash = sha256(binary)
        let cmd = JadeOta(fwsize: firmware.fwsize,
                          cmpsize: binary.count,
                          otachunk: version.jadeOtaMaxChunk,
                          cmphash: hash,
                          patchsize: firmware.patchSize)
        return exchange(JadeRequest(method: firmware.isDelta ? "ota" : "ota_delta",
                                    params: cmd))
            .flatMap { (_: JadeResponse<Bool>) in
                self.otaSend(binary, size: binary.count, chunksize: version.jadeOtaMaxChunk)
            }.flatMap { _ in
                self.exchange(JadeRequest<JadeEmpty>(method: "ota_complete"))
            }.compactMap { (res: JadeResponse<Bool>) in
                return res.result ?? false
            }
    }

    public func send(_ chunk: Data) -> Observable<Bool> {
        return Observable.create { observer in
            return self.exchange(JadeRequest(method: "ota_data", params: chunk))
                .subscribe(onNext: { (res: JadeResponse<Bool>) in
                    observer.onNext(res.result ?? false)
                    observer.onCompleted()
                }, onError: { err in
                    observer.onError(err)
                })
        }
    }

    public func otaSend(_ data: Data, size: Int, chunksize: Int = 4 * 1024) -> Observable<Bool> {
        let chunks = stride(from: 0, to: data.count, by: Int(chunksize)).map {
            Array(data[$0 ..< Swift.min($0 + Int(chunksize), data.count)])
        }
        let sequence: [Observable<Bool>] = chunks.map { Observable.just(Data($0)) }
            .map { obs in
                return obs.flatMap { chunk in
                    self.send(chunk)
                }
            }
        return Observable.concat(sequence).takeLast(1)
    }
}
