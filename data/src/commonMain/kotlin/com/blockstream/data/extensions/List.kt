package com.blockstream.data.extensions

fun <E> List<E>.indexOfOrNull(element: E): Int? = indexOf(element).takeIf { it != -1 }