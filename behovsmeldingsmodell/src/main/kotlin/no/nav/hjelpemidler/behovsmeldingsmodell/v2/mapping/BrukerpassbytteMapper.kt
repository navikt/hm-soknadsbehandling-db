package no.nav.hjelpemidler.behovsmeldingsmodell.v2.mapping

import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Brukerpassbytte
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Iso6
import no.nav.hjelpemidler.domain.geografi.Veiadresse
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.domain.person.Personnavn

fun tilBrukerpassbytteV2(v1: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerpassbytte, fnr: Fødselsnummer): Brukerpassbytte {
    val navnTokens = v1.brukersNavn.split(" ")
    val fornavn = navnTokens.dropLast(1).joinToString(" ")
    val etternavn = navnTokens.takeLast(1).joinToString(" ")

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
        hmsArtNr = v1.hjelpemiddel.artnr,
        artikkelnavn = v1.hjelpemiddel.navn,
        iso6Tittel = v1.hjelpemiddel.kategori,
        iso6 = v1.hjelpemiddel.kategorinummer?.let { Iso6(it) },
    )

    return Brukerpassbytte(
        id = v1.id,
        innsendingsdato = v1.dato,
        hjmBrukersFnr = v1.fnr ?: fnr,
        navn = Personnavn(fornavn, etternavn = etternavn),
        folkeregistrertAdresse = folkeregistrertAdresse,
        annenUtleveringsadresse = annenUtleveringsadresse,
        hjelpemiddel = hjelpemiddel,
        bytteårsak = v1.bytteårsak,
        byttebegrunnelse = v1.byttebegrunnelse,
        utleveringsmåte = v1.utleveringsmåte,
    )
}
