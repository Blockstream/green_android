package com.blockstream.green.utils


import app.rive.runtime.kotlin.RiveArtboardRenderer
import app.rive.runtime.kotlin.core.PlayableInstance

abstract class RiveListener : RiveArtboardRenderer.Listener {
    override fun notifyLoop(animation: PlayableInstance) { }

    override fun notifyPause(animation: PlayableInstance) { }

    override fun notifyPlay(animation: PlayableInstance) { }

    override fun notifyStop(animation: PlayableInstance) { }
}