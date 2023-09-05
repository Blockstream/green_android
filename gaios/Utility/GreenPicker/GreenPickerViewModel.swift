import Foundation
import gdk

struct GreenPickerItem {
    var code: String
    var title: String
    var hint: String
}

class GreenPickerViewModel {
    var title: String
    var item: GreenPickerItem?
    var items: [GreenPickerItem]
    var filteredItems: [GreenPickerItem]
    
    init(title: String,
         item: GreenPickerItem?,
         items: [GreenPickerItem]
    ) {
        self.title = title
        self.item = item
        self.items = items
        self.filteredItems = items
    }

    func search(_ txt: String?) {
        filteredItems = []
        items.forEach {
            if let txt = txt, txt.count > 0 {
                if ($0.title).lowercased().contains(txt.lowercased()) {
                    filteredItems.append($0)
                }
            } else {
                filteredItems.append($0)
            }
        }
    }

    func getIdx(_ idx: Int) -> Int {
        item = filteredItems[idx]
        if let selected = (items.filter{ $0.code == item?.code}).first {
            if let index = items.firstIndex(where: {$0.code == selected.code}) {
                return index
            }
        }
        return 0
    }
}

