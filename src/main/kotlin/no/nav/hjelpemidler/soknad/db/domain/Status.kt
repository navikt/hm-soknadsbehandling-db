package no.nav.hjelpemidler.soknad.db.domain

enum class Status {
    VENTER_GODKJENNING, GODKJENT_MED_FULLMAKT, GODKJENT, SLETTET, UTLØPT, ENDELIG_JOURNALFØRT;

    fun isSlettetEllerUtløpt() = this == SLETTET || this == UTLØPT
}
