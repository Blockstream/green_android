package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_add_note
import blockstream_green.common.generated.resources.id_comment
import blockstream_green.common.generated.resources.id_description
import blockstream_green.common.generated.resources.id_save
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.models.sheets.NoteViewModel
import com.blockstream.common.models.sheets.NoteViewModelAbstract
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.sheets.NoteBottomSheet.Companion.setResult
import com.blockstream.compose.utils.OpenKeyboard
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import kotlin.math.min

@Parcelize
data class NoteBottomSheet(
    val greenWallet: GreenWallet,
    val note: String,
    val noteType: NoteType
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NoteViewModel>{
            parametersOf(note, noteType, greenWallet)
        }

        NoteBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }

    companion object {
        @Composable
        fun getResult(fn: (String) -> Unit) =
            getNavigationResult(this::class, fn)

        internal fun setResult(result: String) =
            setNavigationResult(this::class, result)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(
    viewModel: NoteViewModelAbstract,
    onDismissRequest: () -> Unit,
) {

    GreenBottomSheet(
        title = stringResource(when(viewModel.noteType){
            NoteType.Note -> Res.string.id_add_note
            NoteType.Description -> Res.string.id_description
            NoteType.Comment -> Res.string.id_comment
        }),
        viewModel = viewModel,
        sideEffectHandler = {
            if (it is SideEffects.Success) {
                (it.data as? String)?.also {
                    setResult(it)
                }
            }
        },
        onDismissRequest = onDismissRequest) {

        val focusRequester = remember { FocusRequester() }
        OpenKeyboard(focusRequester)

        val note by viewModel.note.collectAsStateWithLifecycle()

        TextField(
            value = note,
            onValueChange = {
                viewModel.note.value = it.substring(0 until it.length.coerceAtMost(200))
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = {
                Text(
                    stringResource(
                        when (viewModel.noteType) {
                            NoteType.Note -> Res.string.id_add_note
                            NoteType.Description -> Res.string.id_description
                            NoteType.Comment -> Res.string.id_comment
                        }
                    )
                )
            },
            maxLines = 5,
            trailingIcon = {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "clear text",
                    modifier = Modifier
                        .clickable {
                            viewModel.note.value = ""
                        }
                )
            }, supportingText = {
                Text(
                    text = "${note.length} / 200",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
        )

        GreenButton(
            text = stringResource(Res.string.id_save),
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.postEvent(Events.Continue)
        }
    }
}