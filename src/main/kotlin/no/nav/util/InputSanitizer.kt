package no.nav.util

import java.text.Normalizer

object InputSanitizer {
    private const val ZERO_WIDTH_JOINER = 0x200D
    private val EMOJI_TAGS = 0xE0020..0xE007F

    @JvmStatic
    fun sanitize(input: String?): String? {
        if (input == null) return null
        val normalisert = Normalizer.normalize(input, Normalizer.Form.NFC)
        return buildString(normalisert.length) {
            normalisert.codePoints()
                .filter { erTillatt(it) }
                .forEach { appendCodePoint(it) }
        }
    }

    private fun erTillatt(cp: Int): Boolean {
        if (cp == '\n'.code || cp == '\r'.code || cp == '\t'.code) return true
        return when (Character.getType(cp).toByte()) {
            Character.CONTROL, Character.SURROGATE, Character.PRIVATE_USE -> false
            Character.FORMAT -> cp == ZERO_WIDTH_JOINER || cp in EMOJI_TAGS
            else -> true
        }
    }
}
