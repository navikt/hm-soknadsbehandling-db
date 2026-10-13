package no.nav.hjelpemidler.soknad.db.rapportering

import io.ktor.server.plugins.di.DependencyRegistry
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.GcpEnvironment
import java.util.concurrent.Executors

fun DependencyRegistry.scheduler() {
    provide<LeaderElection> {
        if (Environment.current is GcpEnvironment) {
            NaisLeaderElection(engine = resolve())
        } else {
            LocalLeaderElection
        }
    }
    provide {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        JobbScheduler(
            scheduler,
            leaderElection = resolve(),
            slack = resolve(),
        )
    }
    provide {
        Rapporteringsjobber(
            jobbScheduler = resolve(),
            ManglendeOppgaver(
                transaction = resolve(),
                slack = resolve(),
            ),
            ManglendeBrukerbekreftelse(
                transaction = resolve(),
                epost = resolve(),
                clock = resolve(),
            ),
        )
    }
}

object LocalLeaderElection : LeaderElection {
    override suspend fun erLeder(): Boolean = false
}
