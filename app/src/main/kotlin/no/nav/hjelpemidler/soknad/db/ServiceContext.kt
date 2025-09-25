package no.nav.hjelpemidler.soknad.db

import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.ordre.OrdreService
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.db.safselvbetjening.Safselvbetjening
import no.nav.hjelpemidler.soknad.db.soknad.SøknadService
import no.nav.hjelpemidler.soknad.db.store.Transaction

class ServiceContext(
    transaction: Transaction,
    grunndataClient: GrunndataClient,
    rolleClient: RolleClient,
    val safselvbetjening: Safselvbetjening,
    val metrics: Metrics = Metrics(transaction),
) {
    val ordreService = OrdreService(transaction, grunndataClient)
    val rolleService = RolleService(rolleClient)
    val søknadService = SøknadService(transaction)
}
