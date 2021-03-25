package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import com.blockstream.green.R
import java.util.*

class RecoveryPhraseKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener, View.OnLongClickListener {

    interface OnRecoveryPhraseKeyboardListener {
        fun onRecoveryPhraseStateUpdate(state: RecoveryPhraseState)
    }

    private var state = RecoveryPhraseState(mutableListOf(), -1)

    private var listener: OnRecoveryPhraseKeyboardListener? = null
    private var wordList: List<String>? = null
    private val letterKeys = ArrayList<Button>()
    private val wordsKeys = ArrayList<Button>()
    private val deleteButton: ImageButton

    init {
        LayoutInflater.from(context).inflate(R.layout.recovery_phrase_keyboard_view, this, true)
        deleteButton = findViewById(R.id.delete)
        setButtonListeners(this)
        updateKeyboard(false)
    }

    private fun setButtonListeners(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            if (view is ViewGroup) {
                setButtonListeners(view)
            } else if (view is Button || view is ImageButton) {
                when (view.id) {
                    R.id.delete -> {
                        view.setOnLongClickListener(this)
                    }
                    R.id.word1, R.id.word2, R.id.word3, R.id.word4 -> wordsKeys.add(view as Button)
                    else -> letterKeys.add(view as Button)
                }
                view.setOnClickListener(this)
            }
        }
    }


    private fun setWord(word: CharSequence) {
        state.setWord(word)

        updateKeyboard(false)
    }

    private fun updateKeyboard(autoAddIfFound: Boolean) {
        val enabledKeys = hashSetOf<CharSequence>()
        val matchedWords = mutableListOf<String>()

        val activeWord = state.activeWord() ?: ""
        val activeWordLen = activeWord.length

        wordList?.let { wordList ->
            for (word in wordList) {
                if (word.startsWith(activeWord)) {
                    matchedWords += word
                    if (activeWordLen < word.length) {
                        enabledKeys.add(word.substring(activeWordLen, activeWordLen + 1))
                    }
                }
            }
            if (autoAddIfFound && matchedWords.size == 1 && matchedWords.contains(activeWord)) {
                setWord(activeWord)
                return
            }
        }

        for (key in letterKeys) {
            key.isEnabled = enabledKeys.contains(key.text)
        }

        for (x in wordsKeys.indices) {
            val wordButton = wordsKeys[x]
            if (activeWordLen > 0 && x < matchedWords.size) {
                wordButton.text = matchedWords[x]
                wordButton.visibility = VISIBLE
            } else {
                wordButton.visibility = if (x == 0) INVISIBLE else GONE
            }
        }

        listener?.onRecoveryPhraseStateUpdate(state)
    }

    fun setOnRecoveryPhraseKeyboardListener(listener: OnRecoveryPhraseKeyboardListener?) {
        this.listener = listener
    }

    fun setWordList(wordList: List<String>?) {
        this.wordList = wordList
        updateKeyboard(false)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.delete -> {
                state.deleteCharacter()

                updateKeyboard(false)
            }
            R.id.word1, R.id.word2, R.id.word3, R.id.word4 -> setWord((view as Button).text)
            else -> {
                state.setCharacter((view as Button).text)

                updateKeyboard(true)
            }
        }

        // Vibrate
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun onLongClick(v: View?): Boolean {
        state.removeWord()

        updateKeyboard(false)
        return true
    }

    fun setRecoveryPhraseState(recoveryPhraseState: RecoveryPhraseState) {
        state = recoveryPhraseState
        updateKeyboard(false)
    }

    fun toggleActiveWord(position: Int) {

        state.setActiveWord(if (state.activeIndex == position) -1 else position)

        updateKeyboard(false)
    }

    data class RecoveryPhraseState(
        val phrase: MutableList<CharSequence>,
        var activeIndex: Int
    ) {
        companion object {
            fun empty() = RecoveryPhraseState(mutableListOf(), -1)

            fun fromString(mnemonic: String?): RecoveryPhraseState {

                if(mnemonic == null) return empty()

                // replace new line, and multiple spaces
                val list = mnemonic
                    .replace("\n", " ")
                    .replace("\\s+", "")
                    .split(" ")

                // Its a basic check to prevent huge inputs
                // TODO Validate words
                return if (list.size <= 32){
                    RecoveryPhraseState(list.toMutableList(), -1)
                }else{
                    empty()
                }
            }
        }

        init {
            setActiveWord(activeIndex)
        }

        val isEditMode
            get() = activeIndex != -1

        fun setActiveWord(position: Int) {
            activeIndex = if (position < phrase.size){
                position
            }else{
                -1
            }
        }

        fun setCharacter(char: CharSequence){
            if(hasActiveWord()){
                phrase[activeIndex] = phrase[activeIndex].toString() + char
            }else{
                phrase.add(char)
                activeIndex = phrase.size - 1
            }
        }

        fun deleteCharacter(){
            if(hasActiveWord()){
                val word = activeWord()
                if(word.isNullOrBlank()){
                    removeWord()
                }else{
                    phrase[activeIndex] = word.let{
                        it.substring(0 until it.length - 1)
                    }
                }

                // Case where you are on the last empty word, its better on that case to completely
                // remove the word, that way we can imediatelly show the paste buttno
                if(phrase.size == 1 && phrase[0].isEmpty()){
                    deleteCharacter()
                }

            }else{
                activeIndex = phrase.size - 1
            }
        }

        fun hasActiveWord(): Boolean{
            return activeIndex >= 0 && activeIndex < phrase.size
        }

        fun activeWord(): CharSequence? = if(hasActiveWord()) phrase[activeIndex] else null

        fun setWord(word: CharSequence){

            if(hasActiveWord()){
                phrase[activeIndex] = word
            }else{
                phrase.add(word)
            }

            activeIndex = -1
        }

        fun removeWord(){
            if(hasActiveWord()){
                phrase.removeAt(activeIndex)
            }else{
                phrase.removeLastOrNull()
            }
            activeIndex = -1
        }

        fun toMnemonic(): String {
            return phrase.joinToString(" ")
        }

        override fun toString(): String {
            return toMnemonic()
        }
    }
}