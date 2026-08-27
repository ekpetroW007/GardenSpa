package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.presentation.UrlAnnotationTag
import ru.samates.gardenspa.presentation.linkifiedString

class LinkifiedTextTest {
    @Test
    fun recognizesSeveralUrlsWithoutTrailingPunctuation() {
        val text = linkifiedString(
            "Источники: https://pionray.ru/catalog/botritis2. " +
                "Набор: https://pionray.ru/market3/tproduct/1150216401-504059333372-sistema-pitaniya-dlya-pionov"
        )

        val urls = text.getStringAnnotations(UrlAnnotationTag, 0, text.length).map { it.item }

        assertEquals(
            listOf(
                "https://pionray.ru/catalog/botritis2",
                "https://pionray.ru/market3/tproduct/1150216401-504059333372-sistema-pitaniya-dlya-pionov"
            ),
            urls
        )
    }
}
