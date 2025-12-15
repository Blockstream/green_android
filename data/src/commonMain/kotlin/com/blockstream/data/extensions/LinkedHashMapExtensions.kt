package com.blockstream.data.extensions

fun <K, V> MutableMap<K, V>.toSortedLinkedHashMap(comparator: Comparator<K>): LinkedHashMap<K, V> {
    val sorted = linkedMapOf<K, V>()

    keys.sortedWith(comparator).forEach { k ->
        get(k)?.also { v ->
            sorted[k] = v
        }
    }

    return sorted
}