package no.nav.dagpenger.herald.kafka

import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal interface Topic<K, V> {
    fun publiser(melding: V) {}
    fun publiser(nøkkel: K, melding: V) {}
}

internal class KafkaTopic<K, V> private constructor(
    private val producer: KafkaProducer<K, V>,
    private val topic: String
) : Topic<K, V> {
    companion object {
        private val logger = KotlinLogging.logger {}

        fun jsonTopic(producer: KafkaProducer<String, String>, topic: String) =
            KafkaTopic<String, String>(producer, topic)
    }

    override fun publiser(nøkkel: K, melding: V) {
        producer.send(ProducerRecord(topic, nøkkel, melding))
            .also { logger.info { "Sender ut $melding" } }
    }
}
