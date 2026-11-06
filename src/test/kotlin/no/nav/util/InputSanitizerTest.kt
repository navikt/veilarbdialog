package no.nav.util

import org.junit.jupiter.api.Test

import org.assertj.core.api.Assertions.assertThat

class InputSanitizerTest {
    @Test
    fun `sanitize - tekst med gyldige bokstaver, tegn og tall - fjerner ingenting`() {
        val input = """
            Dette er en test! 1234, med gyldige tegn og bokstaver. #, &, %, +, -, @, * og $ må være lov. "Hei", æøå, §, /, |, [, ', \ og € og kafé er lov.
        """.trimIndent()

        val sanitized = InputSanitizer.sanitize(input)

        assertThat(sanitized).isEqualTo(input)
    }

    @Test
    fun `sanitize - tekst med gyldige tegn og emojis - fjerner ingenting`() {
        val input = """
            Dette er en test med gyldige tegn og emojis. 😊, 😂, 🙂, 🎉, ☀️, ❤️, ✅, 👍, 👍🏽, 👋🏻, 🙏🏿, 🧑‍💻, 🧑🏾‍🍳, 👨‍👩‍👧, 🇳🇴, 🏴󠁧󠁢󠁳󠁣󠁴󠁿 er lov.
        """.trimIndent()

        val sanitized = InputSanitizer.sanitize(input)

        assertThat(sanitized).isEqualTo(input)
    }

    @Test
    fun `sanitize - tekst med gyldige tegn og linjeskift - fjerner ingenting og beholder linjeskift`() {
        val input = """
            Dette er en test! 1234, med gyldige tegn og bokstaver. 
            
            Her har vi et nytt avsnitt. 
        """.trimIndent()

        val sanitized = InputSanitizer.sanitize(input)

        assertThat(sanitized).isEqualTo(input)
    }

    @Test
    fun `sanitize - tekst med gyldige bokstaver og kontrolltegn - fjerner kontrolltegn`() {
        val input = "Dette er en\u0000 test\u001B som\u0007 inneholder\u007F kontrolltegn.\u009B"

        val sanitized = InputSanitizer.sanitize(input)

        assertThat(sanitized).isEqualTo("Dette er en test som inneholder kontrolltegn.")
    }

    @Test
    fun `sanitize - tekst med gyldige bokstaver og usynlig formatteringstegn - fjerner formatteringstegn`() {
        val input = "\uFEFFDette\u200B er en\u202E test\u202C som\u2066 inneholder\u2069 usynlige\u2060 formatteringstegn.\u00AD"

        val sanitized = InputSanitizer.sanitize(input)

        assertThat(sanitized).isEqualTo("Dette er en test som inneholder usynlige formatteringstegn.")
    }

    @Test
    fun `sanitize - tekst med gyldige bokstaver og halve surrogatpar - fjerner halve surrogatpar`() {
        val input = "Dette er en\uD83D test som\uDE00 inneholder halve\uDBFF surrogatpar.\uDC00"

        val sanitized = InputSanitizer.sanitize(input)

        assertThat(sanitized).isEqualTo("Dette er en test som inneholder halve surrogatpar.")
    }

    @Test
    fun `sanitize - tekst med gyldige bokstaver og private use-tegn - fjerner private use-tegn`() {
        val input = "Dette er en\uE000 test som\uF8FF inneholder private\uDB80\uDC00 use-tegn.\uDBFF\uDFFD"

        val sanitized = InputSanitizer.sanitize(input)

        assertThat(sanitized).isEqualTo("Dette er en test som inneholder private use-tegn.")
    }
}