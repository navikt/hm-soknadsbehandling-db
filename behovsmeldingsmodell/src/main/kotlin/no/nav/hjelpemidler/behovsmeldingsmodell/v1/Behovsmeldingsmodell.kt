package no.nav.hjelpemidler.behovsmeldingsmodell.v1

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Behovsmeldingsmodell(val value: KClass<*>)
