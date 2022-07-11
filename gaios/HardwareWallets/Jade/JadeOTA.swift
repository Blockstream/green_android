import Foundation
import RxSwift
import RxBluetoothKit
import CommonCrypto

struct Firmware: Codable {
    enum CodingKeys: String, CodingKey {
        case filename = "filename"
        case version = "version"
        case config = "config"
        case fwsize = "fwsize"
        case fromVersion = "from_version"
        case fromConfig = "from_config"
        case patchSize = "patch_size"
    }
    let filename: String
    let version: String
    let config: String
    let fwsize: Int
    let fromVersion: String?
    let fromConfig: String?
    let patchSize: Int?

    func upgradable(_ jadeVersion: String) -> Bool {
        return self.config == "ble" && self.version > jadeVersion &&
        self.fromConfig ?? "ble" == "ble" && self.fromVersion ?? jadeVersion == jadeVersion
    }

    var isDelta: Bool {
        return patchSize == nil
    }
}

struct FirmwareVersions: Codable {
    let full: [Firmware]?
    let delta: [Firmware]?
}

struct FirmwareChannels: Codable {
    let beta: FirmwareVersions?
    let stable: FirmwareVersions?
    let previous: FirmwareVersions?
}

class JadeOTA: JadeChannel {

    static let MIN_ALLOWED_FW_VERSION = "0.1.24"
    static let FW_SERVER_HTTPS = "https://jadefw.blockstream.com"
    static let FW_SERVER_ONION = "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion"
    static let FW_JADE_PATH = "/bin/jade/"
    static let FW_JADEDEV_PATH = "/bin/jadedev/"
    static let FW_JADE1_1_PATH = "/bin/jade1.1/"
    static let FW_JADE1_1DEV_PATH = "/bin/jade1.1dev/"
    static let BOARD_TYPE_JADE = "JADE"
    static let BOARD_TYPE_JADE_V1_1 = "JADE_V1.1"
    static let FEATURE_SECURE_BOOT = "SB"

    // Check Jade fmw against minimum allowed firmware version
    func isJadeFwValid(_ version: String) -> Bool {
        return JadeOTA.MIN_ALLOWED_FW_VERSION <= version
    }

    func download(_ fwpath: String, base64: Bool = false) -> [String: Any]? {
        let params: [String: Any] = [
            "method": "GET",
            "accept": base64 ? "base64": "json",
            "urls": ["\(JadeOTA.FW_SERVER_HTTPS)\(fwpath)",
                     "\(JadeOTA.FW_SERVER_ONION)\(fwpath)"] ]
        return Jade.shared.gdkRequestDelegate?.httpRequest(params: params)
    }

    func firmwarePath(_ verInfo: JadeVersionInfo) -> String? {
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

    func firmwareData(_ verInfo: JadeVersionInfo) throws -> Firmware {
        // Get relevant fmw path (or if hw not supported)
        guard let fwPath = firmwarePath(verInfo) else {
            throw JadeError.Abort("Unsupported hardware")
        }
        guard let res = download("\(fwPath)index.json"),
              let body = res["body"] as? [String: Any],
              let json = try? JSONSerialization.data(withJSONObject: body, options: []),
              let channels = try? JSONDecoder().decode(FirmwareChannels.self, from: json) else {
            throw JadeError.Abort("Failed to fetch firmware index")
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
        throw JadeError.Abort("No newer firmware found")
    }

    func getBinary(_ verInfo: JadeVersionInfo, _ fmw: Firmware) throws -> Data {
        guard let fwPath = firmwarePath(verInfo) else {
            throw JadeError.Abort("Unsupported hardware")
        }
        if let res = download("\(fwPath)\(fmw.filename)", base64: true),
            let body = res["body"] as? String,
            let data = Data(base64Encoded: body) {
            return data
        }
        throw JadeError.Abort("Error downloading firmware file")
    }

    func sha256(_ data: Data) -> Data {
        let length = Int(CC_SHA256_DIGEST_LENGTH)
        var hash = [UInt8](repeating: 0, count: length)
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
        }
        return Data(hash)
    }

    func updateFirmware(_ verInfo: JadeVersionInfo, _ fmw: Firmware) -> Observable<Bool> {
        guard let binary = try? getBinary(verInfo, fmw) else {
            return Observable.error(JadeError.Abort("Error downloading firmware file"))
        }
        let cmd = JadeOta(fwsize: fmw.fwsize,
                          cmpsize: binary.count,
                          otachunk: verInfo.jadeOtaMaxChunk,
                          cmphash: sha256(binary),
                          patchsize: fmw.patchSize)
        return exchange(JadeRequest(method: fmw.isDelta ? "ota" : "ota_delta",
                                    params: cmd))
            .flatMap { (_: JadeResponse<Bool>) in
                self.otaSend(binary, size: binary.count, chunksize: verInfo.jadeOtaMaxChunk)
            }.flatMap { _ in
                self.exchange(JadeRequest<JadeEmpty>(method: "ota_complete"))
            }.compactMap { (res: JadeResponse<Bool>) in
                return res.result ?? false
            }
    }

    func send(_ chunk: Data) -> Observable<Bool> {
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

    func otaSend(_ data: Data, size: Int, chunksize: Int = 4 * 1024) -> Observable<Bool> {
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
