package com.msmobile.visitas.extension

fun <T> List<T>.subListInclusive(fromIndex: Int, toIndex: Int): List<T> {
    return subList(fromIndex, toIndex + 1)
}