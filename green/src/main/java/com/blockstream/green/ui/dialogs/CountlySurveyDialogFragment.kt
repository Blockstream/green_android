package com.blockstream.green.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import com.blockstream.green.R
import com.blockstream.green.data.CountlyWidget
import com.blockstream.green.databinding.CountlySurveyDialogBinding
import com.blockstream.green.extensions.snackbar
import dagger.hilt.android.AndroidEntryPoint
import ly.count.android.sdk.ModuleFeedback
import mu.KLogging

@AndroidEntryPoint
class CountlySurveyDialogFragment : AbstractDialogFragment<CountlySurveyDialogBinding>() {

    override fun inflate(layoutInflater: LayoutInflater): CountlySurveyDialogBinding =
        CountlySurveyDialogBinding.inflate(layoutInflater)

    override val screenName: String = "Survey"

    private var widgetOrNull: CountlyWidget? = null

    private val widget: CountlyWidget get() = widgetOrNull!!

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

        binding.isLoading = true

        countly.feedbackWidget?.takeIf { it.type == ModuleFeedback.FeedbackWidgetType.survey }?.also {
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
            logger.info { "Widget does not exists or it's not a Survey" }
            dismiss()
            return
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.toggleRating.addOnButtonCheckedListener { _, _, _ ->
            checkRequirements()
        }

        binding.buttonSubmit.setOnClickListener {
            submit()
        }

        binding.textInputEditText.addTextChangedListener {
            checkRequirements()
        }

        binding.buttonNotNow.setOnClickListener {
            isNotNow = true
            dismiss()
        }
    }

    private fun checkRequirements(){
        widgetOrNull?.also { widget ->
            binding.isSubmitEnabled =
                !(widget.rating?.required == true && binding.toggleRating.checkedButtonId == View.NO_ID) && !(widget.text?.required == true && binding.textInputEditText.text?.isBlank() == true)
        }
    }

    private fun submit() {
        isSubmitted = true

        val data = mutableMapOf<String, Any>()

        widget.rating?.also {
            when (binding.toggleRating.checkedButtonId) {
                R.id.button1 -> 1
                R.id.button2 -> 2
                R.id.button3 -> 3
                R.id.button4 -> 4
                R.id.button5 -> 5
                else -> null
            }?.also { rating ->
                data["answ-" + it.id] = rating
            }
        }


        widget.text?.let {
            data["answ-" + it.id] = binding.textInputEditText.text.toString()
        }

        countly.sendFeedbackWidgetData(widget.widget, data)

        activity?.snackbar(widget.msg.thanks)

        dismiss()
    }

    private fun updateUI() {
        binding.isLoading = false

        binding.title = widget.name

        // Rating
        binding.rating = widget.rating?.question
        binding.likely = widget.rating?.likely
        binding.notLikely = widget.rating?.notLikely

        // Text
        binding.text = widget.text?.question
        binding.followUpInput = widget.text?.followUpInput

        checkRequirements()
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
            showSingle(CountlySurveyDialogFragment(), fragmentManager)
        }
    }
}
