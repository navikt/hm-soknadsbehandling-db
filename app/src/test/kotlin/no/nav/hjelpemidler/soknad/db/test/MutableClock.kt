package no.nav.hjelpemidler.soknad.db.test

import no.nav.hjelpemidler.time.ZONE_ID_EUROPE_OSLO
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MutableClock(
    private var instant: Instant = Instant.now(),
    private var zone: ZoneId = ZONE_ID_EUROPE_OSLO,
) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant

    fun plusDays(days: Int): MutableClock {
        instant = instant.plus(days.toLong(), ChronoUnit.DAYS)
        return this
    }

    fun minusDays(days: Int): MutableClock {
        instant = instant.minus(days.toLong(), ChronoUnit.DAYS)
        return this
    }

    fun plusMinutes(minutes: Int): MutableClock {
        instant = instant.plus(minutes.toLong(), ChronoUnit.MINUTES)
        return this
    }
}
