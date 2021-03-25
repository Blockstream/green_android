package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;
import com.google.common.base.Charsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MnemonicHelper {
    public static final ArrayList<String> mWordsArray;
    public static final Set<String> mWords;

    static {
        mWordsArray = new ArrayList<>(Wally.BIP39_WORDLIST_LEN);
        mWords = new HashSet<>(Wally.BIP39_WORDLIST_LEN);
        initWordList(mWordsArray, mWords);
    }

    public static void initWordList(final ArrayList<String> wordsArray, final Set<String> words) {
        final Object en = Wally.bip39_get_wordlist("en");
        for (int i = 0; i < Wally.BIP39_WORDLIST_LEN; ++i) {
            wordsArray.add(Wally.bip39_get_word(en, i));
            if (words != null)
                words.add(wordsArray.get(i));
        }
    }
}
