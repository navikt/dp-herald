package no.nav.dagpenger.herald

import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding

val config = EnvironmentVariables() overriding
    systemProperties()
