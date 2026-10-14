package no.nav.hjelpemidler.soknad.db.client.safselvbetjening

import com.expediagroup.graphql.client.converter.ScalarConverter
import no.nav.hjelpemidler.time.toOffsetDateTime
import java.time.LocalDateTime
import java.time.OffsetDateTime

class DateTimeScalarConverter : ScalarConverter<OffsetDateTime> {
    override fun toScalar(rawValue: Any): OffsetDateTime = LocalDateTime.parse(rawValue.toString()).toOffsetDateTime()
    override fun toJson(value: OffsetDateTime): Any = value.toLocalDateTime().toString()
}
