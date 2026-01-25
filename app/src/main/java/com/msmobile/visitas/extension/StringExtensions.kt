package com.msmobile.visitas.extension

import java.text.Normalizer

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

private fun CharSequence.removeAccents(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

fun String.containsAllWords(text: String): Boolean {
    return this.isNotEmpty()
            && text.isNotEmpty()
            && text.removeAccents()
        .split(" ")
        .all { word ->
            this.removeAccents().contains(word, true)
        }
}

fun String.containsAnyWords(text: String): Boolean {
    return text.removeAccents().split(" ").any { word -> this.removeAccents().contains(word, true) }
}

/**
 * Splits this char sequence to a list of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param transformer Allows to modify the list before returning it.
 */
fun String.split(
    vararg delimiters: Char,
    transformer: MutableList<String>.() -> Unit
): List<String> {
    val list = split(*delimiters).toMutableList().apply(transformer)
    return list
}