package no.nav.hjelpemidler.soknad.db.soknad

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.hjelpemidler.serialization.kotlinx.UUIDSerializer
import java.util.UUID

@Resource("/soknad")
class Søknader {
    @Resource("/bruker")
    class Bruker(val parent: Søknader = Søknader()) {
        @Resource("/{soknadId}")
        class SøknadId(
            @Serializable(with = UUIDSerializer::class) @SerialName("soknadId") val søknadId: UUID,
            val parent: Bruker = Bruker(),
        )
    }

    @Resource("/innsender")
    class Innsender(val parent: Søknader = Søknader()) {
        @Resource("/{soknadId}")
        class SøknadId(
            @Serializable(with = UUIDSerializer::class) @SerialName("soknadId") val søknadId: UUID,
            val parent: Innsender = Innsender(),
        )
    }

    @Resource("/{soknadId}")
    class SøknadId(
        @Serializable(with = UUIDSerializer::class) @SerialName("soknadId") val søknadId: UUID,
        val parent: Søknader = Søknader(),
    ) {
        @Resource("/journalpost")
        class Journalpost(val parent: SøknadId) {
            constructor(søknadId: UUID) : this(SøknadId(søknadId))
        }

        @Resource("/oppgave")
        class Oppgave(val parent: SøknadId) {
            constructor(søknadId: UUID) : this(SøknadId(søknadId))
        }

        @Resource("/ordre")
        class Ordre(val parent: SøknadId) {
            constructor(søknadId: UUID) : this(SøknadId(søknadId))
        }

        @Resource("/sak")
        class Sak(val parent: SøknadId) {
            constructor(søknadId: UUID) : this(SøknadId(søknadId))
        }

        @Resource("/status")
        class Status(val parent: SøknadId) {
            constructor(søknadId: UUID) : this(SøknadId(søknadId))
        }

        @Resource("/vedtaksresultat")
        class Vedtaksresultat(val parent: SøknadId) {
            constructor(søknadId: UUID) : this(SøknadId(søknadId))
        }
    }
}

/**
 * V2 av @Resource("/soknad") som benytter v2 av datamodellen (Innsenderbehovsmelding)
 */
@Resource("/behovsmelding")
class Behovsmelding {
    @Resource("/{behovsmeldingId}")
    class BehovsmeldingId(
        @Serializable(with = UUIDSerializer::class) @SerialName("behovsmeldingId") val behovsmeldingId: UUID,
        val parent: Behovsmelding = Behovsmelding(),
    )

    @Resource("/{behovsmeldingId}/metadata")
    class BehovsmeldingMetadata(
        @Serializable(with = UUIDSerializer::class) @SerialName("behovsmeldingId") val behovsmeldingId: UUID,
        val parent: Behovsmelding = Behovsmelding(),
    )
}

// TODO: Det gir kanskje mer mening å slå i hop med /behovsmelding over, og returnere som JsonNode/BehovsmeldingBase
@Resource("/brukerpassbytte")
class Brukerpassbytte {
    @Resource("/{behovsmeldingId}")
    class BehovsmeldingId(
        @Serializable(with = UUIDSerializer::class) @SerialName("behovsmeldingId") val behovsmeldingId: UUID,
        val parent: Brukerpassbytte = Brukerpassbytte(),
    )
}
