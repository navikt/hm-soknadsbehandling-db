package no.nav.hjelpemidler.soknad.db.domain

enum class Status {
    VENTER_GODKJENNING, GODKJENT_MED_FULLMAKT, GODKJENT, SLETTET, UTLØPT, ENDELIG_JOURNALFØRT,
    VEDTAKSRESULTAT_INNVILGET, VEDTAKSRESULTAT_MUNTLIG_INNVILGET, VEDTAKSRESULTAT_DELVIS_INNVILGET, VEDTAKSRESULTAT_AVSLÅTT, VEDTAKSRESULTAT_ANNET, UTSENDING_STARTET, VEDTAKSRESULTAT_HENLAGTBORTFALT;

    fun isSlettetEllerUtløpt() = this == SLETTET || this == UTLØPT
}
