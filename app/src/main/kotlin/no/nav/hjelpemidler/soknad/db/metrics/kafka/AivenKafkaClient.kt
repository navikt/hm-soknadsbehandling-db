package no.nav.hjelpemidler.soknad.db.metrics.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.configuration.KafkaEnvironmentVariable
import no.nav.hjelpemidler.soknad.db.Configuration
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.TimeUnit

private val logg = KotlinLogging.logger { }

class AivenKafkaClient : KafkaClient {
    private val topic = Configuration.KAFKA_TOPIC
    private val kafkaProducer: KafkaProducer<String, String> = KafkaProducer(producerConfigs())

    private fun producerConfigs(): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaEnvironmentVariable.KAFKA_BROKERS
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[CommonClientConfigs.CLIENT_ID_CONFIG] = "hm-soknadsbehandling-db-v1"
        props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = SecurityProtocol.SSL.name
        props[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
        props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "jks"
        props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = KafkaEnvironmentVariable.KAFKA_TRUSTSTORE_PATH
        props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = KafkaEnvironmentVariable.KAFKA_CREDSTORE_PASSWORD
        props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = KafkaEnvironmentVariable.KAFKA_KEYSTORE_PATH
        props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = KafkaEnvironmentVariable.KAFKA_CREDSTORE_PASSWORD
        return props
    }

    private fun produceEvent(key: String, event: String) {
        try {
            kafkaProducer.send(ProducerRecord(topic, key, event)).get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // Svelger exceptions fordi sending av statistikk ikke er kritisk. Log error slik at noen forhåpentligvis oppdager og fikser problemet.
            logg.error(e) { "Sending til topic: $topic feilet for event: '$event'" }
        }
    }

    override fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        produceEvent(measurement, toEventString(measurement, fields, tags))
    }
}
