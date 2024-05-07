package no.nav.hjelpemidler.behovsmeldingsmodell

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Behovsmeldingsmodell(val value: KClass<*>)
