package no.nav.dagpenger.herald

import no.nav.dagpenger.herald.kafka.AivenConfig
import no.nav.dagpenger.herald.kafka.KafkaTopic.Companion.jsonTopic
import no.nav.dagpenger.herald.tjenester.TmsUtkastHendelserRiver
import no.nav.dagpenger.herald.tjenester.tms_utkast_topic
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import java.util.Properties

private val aivenKafka: AivenConfig = AivenConfig.default

fun main() {
    val env = System.getenv()
    val beskjedTopic by lazy {
        jsonTopic(
            createProducer(aivenKafka.producerConfig(Properties())),
            config[tms_utkast_topic]
        )
    }

    RapidApplication.create(env) { _, rapidsConnection ->
        TmsUtkastHendelserRiver(rapidsConnection, beskjedTopic)
    }.start()
}

private fun <K, V> createProducer(producerConfig: Properties = Properties()) =
    KafkaProducer<K, V>(producerConfig).also { producer ->
        Runtime.getRuntime().addShutdownHook(
            Thread {
                producer.flush()
                producer.close()
            }
        )
    }
