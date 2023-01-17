package com.blockstream.green.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.R
import com.blockstream.green.data.CountlyWidget
import com.blockstream.green.data.FollowUpType
import com.blockstream.green.databinding.CountlyNpsDialogBinding
import com.blockstream.green.utils.snackbar
import dagger.hilt.android.AndroidEntryPoint
import ly.count.android.sdk.ModuleFeedback
import mu.KLogging

@AndroidEntryPoint
class CountlyNpsDialogFragment() : AbstractDialogFragment<CountlyNpsDialogBinding>() {

    override fun inflate(layoutInflater: LayoutInflater): CountlyNpsDialogBinding =
        CountlyNpsDialogBinding.inflate(layoutInflater)

    override val screenName: String = "NPS"

    private var widgetOrNull: CountlyWidget? = null

    private val widget: CountlyWidget get() = widgetOrNull!!

    private var rating = -1
    private var isNotNow = false
    private var isSubmitted = false

    override val isFullWidth: Boolean = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.setCanceledOnTouchOutside(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        countly.feedbackWidget?.takeIf { it.type == ModuleFeedback.FeedbackWidgetType.nps }?.also {
            countly.getFeedbackWidgetData(it) { fetchedWidget ->
                if (fetchedWidget != null) {
                    widgetOrNull = fetchedWidget
                    updateUI()
                } else {
                    snackbar(R.string.id_something_went_wrong)
                    dismiss()
                }
            }
        } ?: run {
            logger.info { "Widget does not exists or it's not a NPS" }
            dismiss()
            return
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.toggleRating.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                rating = when (checkedId) {
                    R.id.button0 -> 0
                    R.id.button1 -> 2
                    R.id.button2 -> 4
                    R.id.button3 -> 6
                    R.id.button4 -> 8
                    R.id.button5 -> 10
                    else -> -1
                }

                if(widget.followUpType == FollowUpType.None){
                    submit()
                }else{
                    updateUI()
                }
            }
        }

        binding.buttonSubmit.setOnClickListener {
            submit()
        }

        binding.buttonNotNow.setOnClickListener {
            isNotNow = true
            dismiss()
        }
    }

    private fun submit() {
        isSubmitted = true

        val data = mutableMapOf<String, Any>()

        data["rating"] = rating

        if (widget.followUpType != FollowUpType.None) {
            data["comment"] = binding.textInputEditText.text.toString()
        }

        countly.sendFeedbackWidgetData(widget.widget, data)

        activity?.snackbar(widget.msg.thanks)

        dismiss()
    }

    private fun updateUI() {
        if (rating == -1) {
            binding.isFollowUp = false
            binding.question = widget.msg.mainQuestion
            binding.likely = widget.appearance.likely
            binding.notLikely = widget.appearance.notLikely
        } else {
            binding.isFollowUp = true
            binding.followUpInput = widget.appearance.followUpInput

            if (widget.followUpType == FollowUpType.Score) {
                binding.question = when (rating) {
                    9, 10 -> widget.msg.followUpPromoter
                    7, 8 -> widget.msg.followUpPassive
                    else -> widget.msg.followUpDetractor
                }
            } else {
                binding.question = widget.msg.followUpAll
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(!isSubmitted && !isNotNow){
            widgetOrNull?.also {
                countly.sendFeedbackWidgetData(it.widget, null)
            }
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager) {
            showSingle(CountlyNpsDialogFragment(), fragmentManager)
        }
    }
}
