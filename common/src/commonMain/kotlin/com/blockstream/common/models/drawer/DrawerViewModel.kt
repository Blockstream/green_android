package com.blockstream.common.models.drawer

import com.blockstream.common.Urls
import com.blockstream.common.events.Events
import com.blockstream.common.models.wallets.WalletsViewModel

class DrawerViewModel: WalletsViewModel(isHome = false){
    class LocalEvents{
        object ClickHelp : Events.OpenBrowser(Urls.HELP_CENTER)
    }
}