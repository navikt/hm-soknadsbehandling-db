package no.nav.hjelpemidler.soknad.db.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT

/**
 * NB! Kun ment som et gyldig JWT.
 */
fun lagJwt(): DecodedJWT = JWT
    .create()
    .sign(Algorithm.HMAC256("test".toByteArray()))
    .let(JWT::decode)
