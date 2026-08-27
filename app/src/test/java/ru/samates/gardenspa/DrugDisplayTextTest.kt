package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.domain.NO_DRUG_REQUIRED_LABEL
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.domain.toDrugDisplayText

class DrugDisplayTextTest {
    @Test
    fun legacyNoDrugLabelIsShownWithClearMeaning() {
        assertEquals(NO_DRUG_REQUIRED_LABEL, "Не требуется".toDrugDisplayName())
        assertEquals(NO_DRUG_REQUIRED_LABEL, "Не требуется".toDrugDisplayText())
    }

    @Test
    fun currentNoDrugLabelIsIdempotent() {
        assertEquals(NO_DRUG_REQUIRED_LABEL, NO_DRUG_REQUIRED_LABEL.toDrugDisplayName())
        assertEquals(NO_DRUG_REQUIRED_LABEL, NO_DRUG_REQUIRED_LABEL.toDrugDisplayText())
    }

    @Test
    fun actualDrugDescriptionIsNotChanged() {
        val description = "Биологический фунгицид без указания бренда"

        assertEquals(description, description.toDrugDisplayName())
        assertEquals("Препарат: $description", description.toDrugDisplayText())
    }
}
