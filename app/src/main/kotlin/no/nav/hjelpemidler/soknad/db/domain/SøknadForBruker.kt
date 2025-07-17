package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Behovsmelding
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerpassbytte
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.mapping.tilBrukerpassbytteV2
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.mapping.tilInnsenderbehovsmeldingV2
import no.nav.hjelpemidler.domain.person.toFødselsnummer
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.client.hmdb.enums.MediaType
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.Product
import java.util.Date
import java.util.UUID

class SøknadForBruker private constructor(
    val søknadId: BehovsmeldingId,
    val behovsmeldingType: BehovsmeldingType,
    val journalpostId: String?,
    val datoOpprettet: Date,
    var datoOppdatert: Date,
    val status: BehovsmeldingStatus,
    val fullmakt: Boolean,
    val fnrBruker: String,
    val brukerpassbyttedata: Brukerpassbytte?,
    val brukerpassbyttedataV2: no.nav.hjelpemidler.behovsmeldingsmodell.v2.Brukerpassbytte?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
    var ordrelinjer: List<SøknadForBrukerOrdrelinje>,
    var fagsakId: String?,
    var søknadType: String?,
    val valgteÅrsaker: List<String>,
    val innsenderbehovsmelding: Innsenderbehovsmelding?,
) {
    companion object {
        fun new(
            søknadId: UUID,
            behovsmeldingType: BehovsmeldingType,
            journalpostId: String?,
            datoOpprettet: Date,
            datoOppdatert: Date,
            behovsmeldingJson: JsonNode,
            behovsmeldingJsonV2: JsonNode?,
            status: BehovsmeldingStatus,
            fullmakt: Boolean,
            fnrBruker: String,
            er_digital: Boolean,
            soknadGjelder: String?,
            ordrelinjer: List<SøknadForBrukerOrdrelinje>,
            fagsakId: String?,
            søknadType: String?,
            valgteÅrsaker: List<String>,
        ): SøknadForBruker {
            return SøknadForBruker(
                søknadId = søknadId,
                behovsmeldingType = behovsmeldingType,
                journalpostId = journalpostId,
                datoOpprettet = datoOpprettet,
                datoOppdatert = datoOppdatert,
                status = status,
                fullmakt = fullmakt,
                fnrBruker = fnrBruker,
                brukerpassbyttedata = when (behovsmeldingType) {
                    BehovsmeldingType.SØKNAD, BehovsmeldingType.BESTILLING, BehovsmeldingType.BYTTE -> null
                    BehovsmeldingType.BRUKERPASSBYTTE -> jsonMapper.treeToValue<Brukerpassbytte>(behovsmeldingJson["brukerpassbytte"])
                },
                brukerpassbyttedataV2 = when (behovsmeldingType) {
                    BehovsmeldingType.SØKNAD, BehovsmeldingType.BESTILLING, BehovsmeldingType.BYTTE -> null
                    BehovsmeldingType.BRUKERPASSBYTTE -> tilBrukerpassbytteV2(
                        jsonMapper.treeToValue<Brukerpassbytte>(
                            behovsmeldingJson["brukerpassbytte"],
                        ),
                        fnrBruker.toFødselsnummer(),
                    )
                },
                er_digital = er_digital,
                soknadGjelder = soknadGjelder,
                ordrelinjer = ordrelinjer,
                fagsakId = fagsakId,
                søknadType = søknadType,
                valgteÅrsaker = valgteÅrsaker,
                innsenderbehovsmelding = when (behovsmeldingType) {
                    BehovsmeldingType.SØKNAD, BehovsmeldingType.BESTILLING, BehovsmeldingType.BYTTE ->
                        behovsmeldingJsonV2?.let { jsonMapper.treeToValue<Innsenderbehovsmelding>(it) }
                            ?: tilInnsenderbehovsmeldingV2(
                                jsonMapper.treeToValue<Behovsmelding>(behovsmeldingJson),
                            )

                    BehovsmeldingType.BRUKERPASSBYTTE -> null
                },
            )
        }

        fun newEmptySøknad(
            søknadId: UUID,
            behovsmeldingType: BehovsmeldingType,
            journalpostId: String?,
            datoOpprettet: Date,
            datoOppdatert: Date,
            status: BehovsmeldingStatus,
            fullmakt: Boolean,
            fnrBruker: String,
            er_digital: Boolean,
            soknadGjelder: String?,
            ordrelinjer: List<SøknadForBrukerOrdrelinje>,
            fagsakId: String?,
            søknadType: String?,
            valgteÅrsaker: List<String>,
        ) = SøknadForBruker(
            søknadId = søknadId,
            behovsmeldingType = behovsmeldingType,
            journalpostId = journalpostId,
            datoOpprettet = datoOpprettet,
            datoOppdatert = datoOppdatert,
            status = status,
            fullmakt = fullmakt,
            fnrBruker = fnrBruker,
            brukerpassbyttedata = null,
            brukerpassbyttedataV2 = null,
            er_digital = er_digital,
            soknadGjelder = soknadGjelder,
            ordrelinjer = ordrelinjer,
            fagsakId = fagsakId,
            søknadType = søknadType,
            valgteÅrsaker = valgteÅrsaker,
            innsenderbehovsmelding = null,
        )
    }
}

data class SøknadForBrukerOrdrelinje(
    val antall: Double,
    val antallEnhet: String,
    val kategori: String?,
    val artikkelBeskrivelse: String,
    val artikkelNr: String,
    val datoUtsendelse: String,

    // val artikkelBeskrivelse: String, <=> artikkelNavn
    // val serieNr: String?,

    var hmdbBeriket: Boolean = false,
    var hmdbProduktNavn: String? = null,
    var hmdbBeskrivelse: String? = null,
    var hmdbKategori: String? = null,
    var hmdbBilde: String? = null,
    var hmdbURL: String? = null,
) {
    fun berik(produkt: Product?): SøknadForBrukerOrdrelinje {
        if (produkt == null) {
            hmdbBeriket = false
            return this
        }
        hmdbBeriket = true
        hmdbProduktNavn = produkt.articleName
        hmdbBeskrivelse = produkt.attributes.text
        hmdbKategori = produkt.isoCategoryTitle
        hmdbURL = produkt.productVariantURL
        hmdbBilde = produkt.media
            .filter { it.type == MediaType.IMAGE }
            .minByOrNull { it.priority }
            ?.uri?.let { "https://finnhjelpemiddel.nav.no/imageproxy/400d/$it" }
        return this
    }
}
