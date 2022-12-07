package no.nav.dagpenger.herald.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.herald.config
import no.nav.dagpenger.herald.kafka.Topic
import no.nav.dagpenger.herald.tjenester.SøknadEndretTilstand.Companion.validate
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.builder.UtkastJsonBuilder
import java.net.URI
import java.util.UUID

internal typealias UtkastTopic = Topic<String, String>

internal val soknad_url by stringType
internal val tms_utkast_topic by stringType

internal class TmsUtkastHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val utkastTopic: UtkastTopic
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.validate() }
        }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val tilstand = packet["gjeldendeTilstand"].asText()

        withLoggingContext(
            "søknad_uuid" to søknadId.toString(),
            "tilstand" to tilstand
        ) {
            logger.info { "Informerer tms-utkast om endringer" }
            val utkast = SøknadEndretTilstand(packet)

            utkastTopic.publiser(
                utkast.nøkkel,
                utkast.utkastJson
            )
        }
    }
}

class SøknadEndretTilstand(packet: JsonMessage) {
    private val søknadId = packet["søknad_uuid"].asUUID()
    private val ident = packet["ident"].asText()
    private val tittel = "Søknad om dagpenger"
    private val link = søknadUrl.resolve("${søknadUrl.path}/$søknadId")
    private val tilstand = packet["gjeldendeTilstand"].asText()
    private val prosessnavn = packet["prosessnavn"].asText()
    val nøkkel get() = søknadId.toString()

    fun skalPubliseres() = tilstand != "Påbegynt" || prosessnavn == "Dagpenger"

    private fun jsonBuilder() = UtkastJsonBuilder().apply {
        withUtkastId(søknadId.toString())
        withIdent(ident)
        withTittel(tittel)
        withLink(link.toASCIIString())
    }

    val utkastJson
        get() = when (tilstand) {
            "Påbegynt" -> jsonBuilder().create()
            "Innsendt" -> jsonBuilder().delete()
            "Slettet" -> jsonBuilder().delete()
            else -> throw IllegalArgumentException("Ukjent tilstand på søknad")
        }

    companion object {
        private val søknadUrl: URI
            get() = URI(config[soknad_url]).also {
                require(it.isAbsolute) { "URL til søknad må være absolutt" }
            }

        fun JsonMessage.validate() {
            demandValue("@event_name", "søknad_endret_tilstand")
            requireKey("søknad_uuid", "ident", "gjeldendeTilstand")
            interestedIn("prosessnavn")
        }
    }
}

private fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
