package com.greenaddress.greenbits.ui;

import com.blockstream.libwally.Wally;
import com.google.common.base.Charsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MnemonicHelper {

    private static int levenshteinDistance(final String sA, final String sB) {
        final int s1 = sA.length() + 1;
        final int s2 = sB.length() + 1;

        int[] c = new int[s1];
        int[] nc = new int[s1];

        for (int j = 0; j < s1; ++j) c[j] = j;

        for (int j = 1; j < s2; ++j) {
            nc[0] = j;
            for(int k = 1; k < s1; ++k)
                nc[k] = Math.min(Math.min(c[k] + 1, nc[k - 1] + 1), c[k - 1]
                        + ((sA.charAt(k - 1) == sB.charAt(j - 1)) ? 0 : 1));
            final int[] swap = c; c = nc; nc = swap;
        }
        return c[s1 - 1];
    }

    static boolean isInvalidWord(final ArrayList<String> words, final String word, final boolean equals) {
        for (final String w : words) {
            if ((!equals && w.startsWith(word)) ||
                    (equals && w.equals(word))) {
                return false;
            }
        }
        return true;
    }

    public static byte[] decryptMnemonic(final byte[] entropy, final String normalizedPassphrase) {
        final byte[] salt = Arrays.copyOfRange(entropy, 32, 36);
        final byte[] encrypted = Arrays.copyOf(entropy, 32);
        final byte[] derived = new byte[64];
        Wally.scrypt(normalizedPassphrase.getBytes(Charsets.UTF_8), salt, 16384, 8, 8, derived);
        final byte[] key = Arrays.copyOfRange(derived, 32, 64);
        final byte[] decrypted = new byte[32];

        Wally.aes(key, encrypted, Wally.AES_FLAG_DECRYPT, decrypted);
        for (int i = 0; i < 32; ++i)
            decrypted[i] ^= derived[i];

        if (!Arrays.equals(Arrays.copyOf(Wally.sha256d(decrypted, null), 4), salt))
            throw new RuntimeException("Invalid checksum");
        return decrypted;
    }

    static String getClosestWord(final ArrayList<String> words, final String word) {

        final List<Integer> scores = new ArrayList<>(words.size());
        for (final String w : words) {
            scores.add(levenshteinDistance(word, w));
        }
        Integer min = Integer.MAX_VALUE;
        final List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < words.size(); ++i) {
            final Integer score = scores.get(i);
            if (score.compareTo(min) < 0) {
                min = score;
                matches.clear();
                matches.add(i);
            } else if (score.compareTo(min) == 0) {
                matches.add(i);
            }
        }
        for (final Integer m : matches) {
            final String match = words.get(m);
            // give preference to words that start with our word
            if (match.startsWith(word)) {
                return match;
            }
        }
        for (final Integer m : matches) {
            final String match = words.get(m);
            // give preference to words that end with our word
            if (match.endsWith(word)) {
                return match;
            }
        }
        return words.get(matches.get(0));
    }
}
