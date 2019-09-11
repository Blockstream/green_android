import Foundation

extension URL {
    public var queryItems: [String: String] {
        var params = [String: String]()
        return URLComponents(url: self, resolvingAgainstBaseURL: false)?
            .queryItems?
            .reduce([:], { (_, item) -> [String: String] in
                params[item.name] = item.value
                return params
            }) ?? [:]
    }
}

extension URL {
    static func makeFolder(with path: String) -> URL? {
        let fm = FileManager.default
        guard let appDir = fm.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        let folderPath = appDir.appendingPathComponent(path)
        if !fm.fileExists(atPath: folderPath.path) {
            do {
                try fm.createDirectory(at: folderPath, withIntermediateDirectories: true, attributes: nil)
            } catch { return nil }
        }
        return folderPath
    }
}
