package com.blockstream.compose.models.sheets

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewWallet
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

@Serializable
enum class NoteType {
    Note, Description, Comment
}

abstract class NoteViewModelAbstract(val noteType: NoteType, greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    abstract val note: MutableStateFlow<String>
}

class NoteViewModel(initialNote: String, noteType: NoteType, greenWallet: GreenWallet) :
    NoteViewModelAbstract(noteType = noteType, greenWallet = greenWallet) {
    override fun screenName(): String = "TransactionNote"

    override val note: MutableStateFlow<String> = MutableStateFlow(initialNote)

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            postSideEffect(SideEffects.Success(note.value))
            postSideEffect(SideEffects.Dismiss)
        }
    }
}

class NoteViewModelPreview(noteType: NoteType) : NoteViewModelAbstract(noteType = noteType, greenWallet = previewWallet()) {
    override val note: MutableStateFlow<String> = MutableStateFlow("preview")

    companion object {
        fun preview() = NoteViewModelPreview(noteType = NoteType.Note)
    }
}