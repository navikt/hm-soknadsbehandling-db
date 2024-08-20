package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.Personnavn
import no.nav.hjelpemidler.behovsmeldingsmodell.Veiadresse

fun tilBrukerpassbytteV2(v1: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerpassbytte): Brukerpassbytte {
    val fnr = v1.fnr ?: error("Brukerpassbytte ${v1.id} mangler fnr")

    val navnTokens = v1.brukersNavn.split(" ")
    val fornavn = navnTokens.dropLast(1).joinToString { " " }
    val etternavn = navnTokens.takeLast(1).joinToString { " " }

    val folkeregistrertAdresse = v1.folkeregistrertAdresse.let {
        Veiadresse(
            adresse = it.adresse ?: error("Brukerpassbytte ${v1.id} mangler folkeregistrertAdresse.adresse"),
            postnummer = it.postnummer ?: error("Brukerpassbytte ${v1.id} mangler folkeregistrertAdresse.postnummer"),
            poststed = it.poststed ?: error("Brukerpassbytte ${v1.id} mangler folkeregistrertAdresse.poststed"),
        )
    }

    val annenUtleveringsadresse = v1.adresse?.let {
        Veiadresse(
            adresse = it.adresse ?: error("Brukerpassbytte ${v1.id} mangler adresse.adresse"),
            postnummer = it.postnummer ?: error("Brukerpassbytte ${v1.id} mangler adresse.postnummer"),
            poststed = it.poststed ?: error("Brukerpassbytte ${v1.id} mangler adresse.poststed"),
        )
    }

    val hjelpemiddel = Brukerpassbytte.Hjelpemiddel(
        artnr = v1.hjelpemiddel.artnr,
        navn = v1.hjelpemiddel.navn,
        kategori = v1.hjelpemiddel.kategori,
        iso6 = Iso6(v1.hjelpemiddel.kategorinummer),
    )

    return Brukerpassbytte(
        id = v1.id,
        innsendingsdato = v1.dato,
        hjmBrukersFnr = fnr,
        innsendersFnr = fnr,
        navn = Personnavn(fornavn, etternavn),
        folkeregistrertAdresse = folkeregistrertAdresse,
        annenUtleveringsadresse = annenUtleveringsadresse,
        hjelpemiddel = hjelpemiddel,
        bytteårsak = v1.bytteårsak,
        byttebegrunnelse = v1.byttebegrunnelse,
        utleveringsmåte = v1.utleveringsmåte,
    )
}

fun tilBehovsmeldingV2(v1: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerpassbytte): Behovsmelding =
    tilBrukerpassbytteV2(v1)
