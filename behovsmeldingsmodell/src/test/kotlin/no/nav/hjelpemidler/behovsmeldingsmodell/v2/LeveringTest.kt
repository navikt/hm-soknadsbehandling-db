package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class LeveringTest {
    @TestFactory
    fun harFritekstUnderOppfølgingsansvarlig(): List<DynamicTest> = listOf(
        null to false,
        "" to false,
        "\t" to false,
        "oppfølgingsansvarligAnsvarFor" to true,
    ).map {
        val oppfølgingsansvarligAnsvarFor = it.first
        val harFritekstUnderOppfølgingsansvarlig = it.second
        DynamicTest.dynamicTest("Når oppfølgingsansvarligAnsvarFor = '$oppfølgingsansvarligAnsvarFor', så er harFritekstUnderOppfølgingsansvarlig = $harFritekstUnderOppfølgingsansvarlig") {
            lagLevering(oppfølgingsansvarligAnsvarFor = oppfølgingsansvarligAnsvarFor)
                .harFritekstUnderOppfølgingsansvarlig shouldBe harFritekstUnderOppfølgingsansvarlig
        }
    }

    @TestFactory
    fun harFritekstUnderLevering(): List<DynamicTest> = listOf(
        "" to false,
        "\t" to false,
        "utleveringMerknad" to true,
    ).map {
        val utleveringMerknad = it.first
        val harFritekstUnderLevering = it.second
        DynamicTest.dynamicTest("Når utleveringMerknad = '$utleveringMerknad', så er harFritekstUnderLevering = $harFritekstUnderLevering") {
            lagLevering(utleveringMerknad = utleveringMerknad)
                .harFritekstUnderLevering shouldBe harFritekstUnderLevering
        }
    }
}
