package com.kyro.control

class ContextMemory {

    private var lastSubject: String? = null
    private var lastAction: String? = null

    fun updateContext(text: String, extractedEntity: String?) {
        if (extractedEntity != null) {
            lastSubject = extractedEntity
        }
    }

    fun resolvePronouns(text: String): String {
        val pronouns = listOf("unko", "usko", "isko", "him", "her", "them", "it")
        var resolvedText = text.lowercase()

        if (lastSubject != null) {
            for (pronoun in pronouns) {
                // Use regex with word boundaries to prevent replacing sub-strings (e.g. 'withOUT')
                val regex = Regex("\\b$pronoun\\b")
                if (regex.containsMatchIn(resolvedText)) {
                    resolvedText = resolvedText.replace(regex, lastSubject!!)
                }
            }
        }
        return resolvedText
    }

    fun clear() {
        lastSubject = null
        lastAction = null
    }
}
