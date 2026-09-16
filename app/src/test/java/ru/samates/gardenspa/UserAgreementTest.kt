package ru.samates.gardenspa

import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.domain.OfferAcceptance
import ru.samates.gardenspa.domain.OfferDocument

class UserAgreementTest {
    @Test fun acceptanceRequiresVersionExactTextAndDate() {
        val document = OfferDocument("1", "Условия GardenSpa")
        val acceptance = OfferAcceptance("1", document.sha256, 100)
        assertTrue(acceptance.matches(document))
        assertFalse(acceptance.copy(version = "0").matches(document))
        assertFalse(acceptance.copy(sha256 = "").matches(document))
        assertFalse(acceptance.copy(acceptedAt = 0).matches(document))
        assertFalse(acceptance.matches(document.copy(text = "Другие условия")))
        assertEquals(64, document.sha256.length)
        assertEquals(document.sha256, OfferDocument("1", "Условия GardenSpa").sha256)
    }
}
