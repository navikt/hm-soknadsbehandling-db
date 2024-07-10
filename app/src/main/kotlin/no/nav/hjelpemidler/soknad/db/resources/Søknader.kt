package no.nav.hjelpemidler.soknad.db.resources

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.hjelpemidler.soknad.db.serialization.UUIDSerializer
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
}
