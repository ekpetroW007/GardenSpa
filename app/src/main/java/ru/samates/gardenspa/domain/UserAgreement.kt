package ru.samates.gardenspa.domain

import java.security.MessageDigest

const val CURRENT_OFFER_VERSION = "2026-09-16.1"

data class OfferDocument(val version: String, val text: String) {
    val sha256: String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

data class OfferAcceptance(val version: String, val sha256: String, val acceptedAt: Long) {
    fun matches(document: OfferDocument): Boolean =
        acceptedAt > 0 && version == document.version && sha256 == document.sha256
}

data class RegistrationState(val name: String, val canEnter: Boolean)
