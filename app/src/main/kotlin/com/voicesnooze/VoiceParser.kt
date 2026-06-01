package com.voicesnooze

// Sealed class forces exhaustive when() expressions—ensures we handle all command types
sealed class AlarmCommand {
    data class Snooze(val minutes: Int) : AlarmCommand()
    data object Stop : AlarmCommand()
    data object Unknown : AlarmCommand()
}

object VoiceParser {
    // Regex patterns for fuzzy voice matching
    private val snoozePattern = Regex(
        "snooze\\s+(?:for\\s+)?(?:(\\d+)|(?:five|ten|fifteen|twenty|thirty))\\s+(?:minutes?|mins?)",
        RegexOption.IGNORE_CASE
    )
    private val stopPattern = Regex("(?:stop|silence|dismiss|shut\\s+up)", RegexOption.IGNORE_CASE)

    // Maps word numbers to integers (e.g. "five" → 5)
    private val wordToNumber = mapOf(
        "five" to 5, "ten" to 10, "fifteen" to 15,
        "twenty" to 20, "thirty" to 30
    )

    /**
     * Parses raw voice input into a structured AlarmCommand.
     * Examples:
     *   "snooze for 5 minutes" → Snooze(5)
     *   "snooze five minutes" → Snooze(5)
     *   "stop" → Stop
     *   "banana" → Unknown
     */
    fun parse(input: String): AlarmCommand {
        val trimmed = input.trim()

        // Try snooze first
        snoozePattern.find(trimmed)?.let { match ->
            val (numberPart) = match.destructured
            // If regex captured a digit, use it; otherwise look up the word
            val minutes = if (numberPart.isNotEmpty()) {
                numberPart.toInt()
            } else {
                // Find which word number was matched (regex group 2, 3, 4...)
                wordToNumber[trimmed.lowercase().split(Regex("\\s+"))
                    .find { it in wordToNumber }] ?: 5 // Default to 5 if parsing fails
            }
            // Clamp to reasonable range (max 30 min snooze)
            return AlarmCommand.Snooze(minutes.coerceIn(1, 30))
        }

        // Try stop commands
        if (stopPattern.find(trimmed) != null) {
            return AlarmCommand.Stop
        }

        return AlarmCommand.Unknown
    }
}
