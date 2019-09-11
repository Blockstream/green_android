import UIKit
import PromiseKit
import Foundation

class AssetIcon {

    static func saveAssetIcon(at path: String, image: UIImage) {
        guard var iconURL = URL.makeFolder(with: "icons") else { return }
        iconURL = iconURL.appendingPathComponent("\(path).png")
        if let imgData = image.pngData() {
            do {
                try imgData.write(to: iconURL, options: .atomicWrite)
            } catch {
                print(error.localizedDescription)
            }
        } else {
            return
        }
    }

    static func loadAssetIcon(with id: String?) -> UIImage? {
        guard let id = id, let appDir = NSSearchPathForDirectoriesInDomains(FileManager.SearchPathDirectory.documentDirectory,
                                                               FileManager.SearchPathDomainMask.userDomainMask,
                                                               true).first else { return UIImage(named: "default_asset_icon") }
        let iconPath = URL.init(fileURLWithPath: appDir).appendingPathComponent("icons/\(id).png")
        guard let image = UIImage(contentsOfFile: iconPath.path) else { return UIImage(named: "default_asset_icon") }
        return image
    }
}

func getIcons() -> Promise<Any> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) { _ in
        try getSession().refreshAssets(params: ["icons": true, "assets": false])
        }.compactMap(on: bgq) { data in
            guard var icons = data["icons"] as? [String: String] else { return nil }
            if let modIndex = icons.keys.firstIndex(of: "last_modified") {
                icons.remove(at: modIndex)
            }
            for (id, data) in icons {
                if let image = UIImage(base64: data) {
                    AssetIcon.saveAssetIcon(at: id, image: image)
                }
            }
            return nil
    }
}
