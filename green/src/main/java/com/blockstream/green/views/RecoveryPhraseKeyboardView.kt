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
import kotlinx.coroutines.flow.MutableStateFlow

class RecoveryPhraseKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener, View.OnLongClickListener {

    lateinit var recoveryPhrase: MutableStateFlow<List<String>>
    lateinit var activeWord: MutableStateFlow<Int>

    private var wordList: List<String>? = null
    private val letterKeys = ArrayList<Button>()
    private val wordsKeys = ArrayList<Button>()
    private val deleteButton: ImageButton

    init {
        LayoutInflater.from(context).inflate(R.layout.recovery_phrase_keyboard_view, this, true)
        deleteButton = findViewById(R.id.delete)
        setButtonListeners(this)
    }

    fun bridge(recoveryPhrase: MutableStateFlow<List<String>>, activeWord: MutableStateFlow<Int>) {
        this.recoveryPhrase = recoveryPhrase
        this.activeWord = activeWord
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


    private fun setWord(word: String) {
        if(hasActiveWord()){
            recoveryPhrase.value = recoveryPhrase.value.toMutableList().let {
                it[activeWord.value] = word
                it
            }
        }else{
            recoveryPhrase.value = recoveryPhrase.value.toMutableList().let {
                it.add(word)
                it
            }
        }

        activeWord.value = -1

        updateKeyboard(false)
    }

    private fun updateKeyboard(autoAddIfFound: Boolean) {
        val enabledKeys = hashSetOf<CharSequence>()
        val matchedWords = mutableListOf<String>()

        val activeWord = activeWord() ?: ""
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

        // Disable keys for words greater than 27
        if(recoveryPhrase.value.size >= 27 && this.activeWord.value == -1) {
            for (key in letterKeys) {
                key.isEnabled = false
            }
        }else{
            for (key in letterKeys) {
                key.isEnabled = enabledKeys.contains(key.text)
            }
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
    }

    fun setWordList(wordList: List<String>?) {
        this.wordList = wordList
        updateKeyboard(false)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.delete -> {
                deleteCharacter()
                updateKeyboard(false)
            }
            R.id.word1, R.id.word2, R.id.word3, R.id.word4 -> setWord((view as Button).text.toString())
            else -> {
                setCharacter((view as Button).text.toString())
                updateKeyboard(true)
            }
        }

        // Vibrate
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun onLongClick(v: View?): Boolean {
        removeWord()
        updateKeyboard(false)
        return true
    }

    fun toggleActiveWord(position: Int) {
        setActiveWord(if (activeWord.value == position) -1 else position)
        updateKeyboard(false)
    }

    private fun hasActiveWord(): Boolean{
        return activeWord.value >= 0 && activeWord.value < recoveryPhrase.value.size
    }

    private fun deleteCharacter(){
        if(hasActiveWord()){
            val word = activeWord()
            if(word.isNullOrBlank()){
                removeWord()
            }else{
                recoveryPhrase.value = recoveryPhrase.value.toMutableList().let {
                    it[activeWord.value] = word.let {
                        it.substring(0 until it.length - 1)
                    }
                    it
                }
            }

            // Case where you are on the last empty word, its better on that case to completely
            // remove the word, that way we can immediately show the paste button
            if(recoveryPhrase.value.size == 1 && recoveryPhrase.value[0].isEmpty()){
                deleteCharacter()
            }

        }else{
            activeWord.value = recoveryPhrase.value.size - 1
        }
    }

    private fun removeWord(){
        recoveryPhrase.value = recoveryPhrase.value.toMutableList().let {
            if(hasActiveWord()){
                it.removeAt(activeWord.value)
            }else{
                it.removeLastOrNull()
            }
            it
        }

        activeWord.value = -1
    }

    private fun setActiveWord(position: Int) {
        activeWord.value = if (position < recoveryPhrase.value.size){
            position
        }else{
            -1
        }
    }

    private fun setCharacter(char: String) {
        recoveryPhrase.value = recoveryPhrase.value.toMutableList().let {
            if (hasActiveWord()) {
                it[activeWord.value] = it[activeWord.value] + char

            } else {
                it.add(char)
                activeWord.value = it.size - 1
            }
            it
        }
    }

    private fun activeWord(): String? = if(hasActiveWord()) recoveryPhrase.value[activeWord.value] else null
}