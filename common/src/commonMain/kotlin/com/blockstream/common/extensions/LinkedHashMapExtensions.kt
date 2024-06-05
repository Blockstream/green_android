package com.blockstream.common.extensions

fun <K, V> LinkedHashMap<K, V>.toSortedLinkedHashMap(comparator: Comparator<K>): LinkedHashMap<K, V> {
    val sorted = linkedMapOf<K, V>()

    keys.sortedWith(comparator).forEach { k ->
        get(k)?.also { v ->
            sorted[k] = v
        }
    }

    return sorted
}
