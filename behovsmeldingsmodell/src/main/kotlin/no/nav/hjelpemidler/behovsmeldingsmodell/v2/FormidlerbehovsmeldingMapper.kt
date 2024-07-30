package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.Bruksarena
import no.nav.hjelpemidler.behovsmeldingsmodell.Fødselsnummer
import no.nav.hjelpemidler.behovsmeldingsmodell.InnsenderRolle
import no.nav.hjelpemidler.behovsmeldingsmodell.KanIkkeAvhjelpesMedEnklereÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.Oppfølgingsansvarlig
import no.nav.hjelpemidler.behovsmeldingsmodell.SitteputeValg
import no.nav.hjelpemidler.behovsmeldingsmodell.Utleveringsmåte
import no.nav.hjelpemidler.behovsmeldingsmodell.UtlevertType
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Hjelpemiddel
import no.nav.hjelpemidler.behovsmeldingsmodell.ÅrsakForAntall
import java.util.UUID

fun tilFormidlerbehovsmeldingV2(
    v1: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Behovsmelding,
    fnrInnsender: Fødselsnummer,
): Formidlerbehovsmelding {
    val id = v1.id ?: error("Behovsmelding v1 mangler id")
    val v1Bruker = v1.søknad?.bruker ?: error("Behovsmelding $id mangler søknad")

    return Formidlerbehovsmelding(
        id = id,
        type = v1.behovsmeldingType,
        innsendingsdato = v1.søknad?.dato ?: error("Behovsmelding $id mangler dato"),
        bruker = Bruker(
            fnr = v1Bruker.fnr,
            navn = v1Bruker.navn,
            signaturtype = v1Bruker.signaturtype
                ?: error("Behovsmelding $id mangler signaturtype"), // TODO hva var reel signaturtype før det ble innført?
            telefon = v1Bruker.telefon,
            veiadresse = v1Bruker.veiadresse,
            kommunenummer = v1Bruker.kommunenummer,
            brukernummer = v1Bruker.brukernummer,
            kilde = v1Bruker.kilde,
        ),
        brukersituasjon = Brukersituasjon(
            bekreftedeVilkår = v1.søknad.brukersituasjon.bekreftedeVilkår,
            funksjonsnedsettelser = Funksjonsnedsettelser(
                bevegelse = v1.søknad.brukersituasjon.funksjonsnedsettelser.bevegelse,
                kognisjon = v1.søknad.brukersituasjon.funksjonsnedsettelser.kognisjon,
                hørsel = v1.søknad.brukersituasjon.funksjonsnedsettelser.hørsel,
            ),
        ),
        levering = Levering(
            hjelpmiddelformidler = v1.søknad.levering.hjelpemiddelformidler,
            oppfølgingsansvarlig = when (v1.søknad.levering.oppfølgingsansvarlig) {
                Oppfølgingsansvarlig.HJELPEMIDDELFORMIDLER -> no.nav.hjelpemidler.behovsmeldingsmodell.v2.Oppfølgingsansvarlig.HJELPEMIDDELFORMIDLER
                Oppfølgingsansvarlig.ANNEN_OPPFØLGINGSANSVARLIG -> no.nav.hjelpemidler.behovsmeldingsmodell.v2.Oppfølgingsansvarlig.ANNEN_OPPFØLGINGSANSVARLIG
            },
            annenOppfølgingsansvarlig = v1.søknad.levering.annenOppfølgingsansvarlig,
            utleveringsmåte = when (v1.søknad.levering.utleveringsmåte) {
                Utleveringsmåte.FOLKEREGISTRERT_ADRESSE -> no.nav.hjelpemidler.behovsmeldingsmodell.v2.Utleveringsmåte.FOLKEREGISTRERT_ADRESSE
                Utleveringsmåte.ANNEN_BRUKSADRESSE -> no.nav.hjelpemidler.behovsmeldingsmodell.v2.Utleveringsmåte.ANNEN_BRUKSADRESSE
                Utleveringsmåte.HJELPEMIDDELSENTRALEN -> no.nav.hjelpemidler.behovsmeldingsmodell.v2.Utleveringsmåte.HJELPEMIDDELSENTRALEN
                Utleveringsmåte.ALLEREDE_UTLEVERT_AV_NAV -> no.nav.hjelpemidler.behovsmeldingsmodell.v2.Utleveringsmåte.ALLEREDE_UTLEVERT_AV_NAV
                null -> null
            },
            annenUtleveringsadresse = v1.søknad.levering.annenUtleveringsadresse,
            utleveringKontaktperson = v1.søknad.levering.utleveringKontaktperson,
            annenKontaktperson = v1.søknad.levering.annenKontaktperson,
            utleveringMerknad = v1.søknad.levering.utleveringMerknad,
            tilleggsinfo = v1.søknad.levering.tilleggsinfo,

        ),
        innsender = Innsender(
            fnr = fnrInnsender,
            rolle = v1.søknad.innsender?.somRolle ?: InnsenderRolle.FORMIDLER, // TODO Kan vi anta dette?
            kurs = v1.søknad.innsender?.godkjenningskurs ?: emptyList(),
            sjekketUtlånsoversiktForKategorier = v1.søknad.innsender?.tjenestligeBehovForUtlånsoversikt?.map { Iso6(it) }
                ?.toSet() ?: emptySet(),
        ),
        hast = v1.søknad.hast,
        hjelpemidler = Hjelpemidler(
            totaltAntall = v1.søknad.hjelpemidler.totaltAntall,
            hjelpemidler = v1.søknad.hjelpemidler.hjelpemidler.map { tilHjelpemiddelV2(it, id) },
        ),
    )
}

fun tilHjelpemiddelV2(v1: Hjelpemiddel, id: UUID): no.nav.hjelpemidler.behovsmeldingsmodell.v2.Hjelpemiddel {
    return Hjelpemiddel(
        antall = v1.antall,
        produkt = HjelpemiddelProdukt(
            hmsnr = v1.hmsnr,
            navn = v1.navn
                ?: error("Behovsmelding $id mangler hjelpemiddelnavn for ${v1.hmsnr}"), // TODO v1.beskrivelse?
            iso8 = Iso8(v1.produkt?.isocode ?: error("Behovsmelding $id mangler isocode for ${v1.hmsnr}")),
            iso8Navn = v1.produkt.isotitle ?: error("Behovsmelding $id mangler isotitle for ${v1.hmsnr}"),
            rangering = v1.produkt.postrank?.toInt() ?: error("Behovsmelding $id mangler rangering for ${v1.hmsnr}"),
            delkontrakttittel = v1.produkt.aposttitle
                ?: error("Behovsmelding $id mangler delkontrakttittel for ${v1.hmsnr}"),
        ),
        tilbehør = (v1.tilbehør ?: emptyList()).map {
            Tilbehør(
                hmsnr = it.hmsnr,
                navn = it.navn,
                antall = it.antall!!,
                begrunnelse = it.begrunnelse,
                fritakFraBegrunnelseÅrsak = it.fritakFraBegrunnelseÅrsak,
            )
        },
        bytter = v1.bytter,
        bruksarena = v1.bruksarena,
        opplysninger = visningstekster(v1),
    )
}

fun visningstekster(hm: Hjelpemiddel): List<Opplysning> =
    listOf(
        utlevertinfo(hm),
        bruksarena(hm),
        trykksårforebygging(hm),
        begrunnelseLavereRangeringEllerIkkeTilsvarende(hm),
        påkrevdeGodkjenningskurs(hm),
        ersMedKabin(hm),
        årsakForAntall(hm),
        rullestolinfo(hm),
        tilleggsinformasjon(hm),
        ersInfo(hm),
    ).flatten()

private fun utlevertinfo(hm: Hjelpemiddel): List<Opplysning> {
    if (!hm.utlevertFraHjelpemiddelsentralen || hm.utlevertInfo?.utlevertType == null) {
        return emptyList()
    }
    val brukernummer = hm.utlevertInfo.overførtFraBruker
    return opplysninger(
        label = I18n("Utlevert"),
        tekst = when (hm.utlevertInfo.utlevertType) {
            UtlevertType.FREMSKUTT_LAGER -> Tekst(
                I18n(
                    nb = "Utlevert fra fremskutt lager",
                    nn = "Utlevert frå framskote lager",
                ),
            )

            UtlevertType.KORTTIDSLÅN -> Tekst(I18n("Utprøvingslån"))
            UtlevertType.OVERFØRT -> Tekst(
                I18n(
                    nb = "Overført fra annen bruker. Brukernummer: $brukernummer",
                    nn = "Overført frå annan brukar. Brukarnummer: $brukernummer",
                ),
            )

            UtlevertType.ANNET -> Tekst(
                hm.utlevertInfo.annenKommentar ?: error("utlevertInfo.annenKommentar er null"),
            )
        },
    )
}

private fun bruksarena(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.bruksarena.isEmpty()) {
        return emptyList()
    }
    return opplysninger(
        label = I18n("Bruksarena"),
        tekster = tilTekster(
            hm.bruksarena.map {
                when (it) {
                    Bruksarena.EGET_HJEM -> I18n(nb = "I eget hjem.", nn = "I eigen heim.")
                    Bruksarena.EGET_HJEM_IKKE_AVLASTNING -> I18n(
                        nb = "I eget hjem. Ikke avlastningsbolig.",
                        nn = "I eigen heim. Ikkje avlastingsbustad.",
                    )

                    Bruksarena.OMSORGSBOLIG_BOFELLESKAP_SERVICEBOLIG -> I18n(
                        nb = "I omsorgsbolig, bofellesskap eller servicebolig.",
                        nn = "I omsorgsbustad, bufellesskap eller servicebustad.",
                    )

                    Bruksarena.BARNEHAGE -> I18n("I barnehage.")
                    Bruksarena.GRUNN_ELLER_VIDEREGÅENDE_SKOLE -> I18n(
                        nb = "På skolen som grunnskole eller videregående skole.",
                        nn = "På skulen som grunnskule eller vidaregåande skule.",
                    )

                    Bruksarena.SKOLEFRITIDSORDNING -> I18n(
                        nb = "På skolefritidsordning.",
                        nn = "På skulefritidsordning.",
                    )

                    Bruksarena.INSTITUSJON -> I18n(
                        nb = "På institusjon som sykehjem.",
                        nn = "På institusjon som sjukeheim.",
                    )

                    Bruksarena.INSTITUSJON_BARNEBOLIG -> I18n(
                        nb = "På institusjon som sykehjem eller barnebolig.",
                        nn = "På institusjon som sjukeheim eller barnebustad.",
                    )

                    Bruksarena.INSTITUSJON_BARNEBOLIG_KUN_PERSONLIG_BRUK -> I18n(
                        nb = "På institusjon som sykehjem eller barnebolig, og hjelpemiddelet skal kun være til pesonlig bruk.",
                        nn = "På institusjon som sjukeheim eller barnebustad, og hjelpemiddelet skal berre vera til pesonlig bruk.",
                    )
                }
            },
        ),
    )
}

private fun trykksårforebygging(hm: Hjelpemiddel): List<Opplysning> {
    val valgteVilkår = hm.vilkår?.filter { it.avhuket }
    if (valgteVilkår.isNullOrEmpty()) {
        return emptyList()
    }
    return opplysninger(
        label = I18n("Behov"),
        tekster = valgteVilkår.map {
            Tekst(i18n = I18n(it.vilkårstekst), fritekst = it.tilleggsinfo)
        },
    )
}

private fun begrunnelseLavereRangeringEllerIkkeTilsvarende(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.begrunnelse == null) {
        return emptyList()
    }
    val label = if (hm.kanIkkeTilsvarende == true) {
        val rangering = hm.produkt?.postrank?.toInt() ?: error("Klarte ikke parse rangering (postrank)")
        if (rangering > 1) {
            I18n(nb = "Begrunnelse for lavere rangering", nn = "Grunngiving for lågare rangering")
        } else {
            I18n(nb = "Kan ikke ha tilsvarende fordi", nn = "Kan ikkje ha tilsvarande fordi")
        }
    } else {
        I18n(nb = "Begrunnelse", nn = "Grunngiving")
    }
    return opplysninger(label, Tekst(hm.begrunnelse))
}

private fun påkrevdeGodkjenningskurs(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.produkt?.påkrevdGodkjenningskurs == null) {
        return emptyList()
    }
    val kurs = hm.produkt.påkrevdGodkjenningskurs.tittel?.lowercase() ?: error("påkrevdGodkjenningskurs.tittel er null")
    val erVerifisert =
        hm.produkt.påkrevdGodkjenningskurs.formidlersGjennomføring == no.nav.hjelpemidler.behovsmeldingsmodell.HjelpemiddelProdukt.FormidlersGjennomføringAvKurs.GODKJENNINGSKURS_DB
    val erERS =
        hm.produkt.påkrevdGodkjenningskurs.kursId == no.nav.hjelpemidler.behovsmeldingsmodell.HjelpemiddelProdukt.KursId.ELEKTRISK_RULLESTOL.id
    return opplysninger(
        label = I18n("Krav om kurs"),
        tekst = Tekst(
            if (erVerifisert) {
                if (erERS) {
                    I18n(
                        nb = "Det er dokumentert at innsender har fullført og bestått både del 1 (teoretisk) og del 2 (praktisk) av godkjenningskurs $kurs.",
                        nn = "Det er dokumentert at innsendar har fullført og bestått både del 1 (teoretisk) og del 2 (praktisk) av godkjenningskurs $kurs.",
                    )
                } else {
                    I18n(
                        nb = "Det er dokumentert at innsender har fullført og bestått godkjenningskurs $kurs.",
                        nn = "Det er dokumentert at innsendar har fullført og bestått godkjenningskurs $kurs.",
                    )
                }
            } else {
                if (erERS) {
                    I18n(
                        nb = "Kommunal formidler har svart at godkjenningskurs $kurs (del 1 og del 2) er gjennomført. Dokumentasjon av kurs sjekkes i behandling av saken.",
                        nn = "Kommunal formidlar har svart at godkjenningskurs $kurs (del 1 og del 2) er gjennomført. Dokumentasjon av kurs blir sjekka i behandling av saka.",
                    )
                } else {
                    I18n(
                        nb = "Kommunal formidler har svart at godkjenningskurs $kurs er gjennomført. Dokumentasjon av kurs sjekkes i behandling av saken.",
                        nn = "Kommunal formidlar har svart at godkjenningskurs $kurs er gjennomført. Dokumentasjon av kurs blir sjekka i behandling av saka.",
                    )
                }
            },
        ),
    )
}

private fun ersMedKabin(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.elektriskRullestolInfo?.kabin == null) {
        return emptyList()
    }
    val opplysninger = mutableListOf<Opplysning>()
    if (hm.elektriskRullestolInfo.kabin.brukerOppfyllerKrav) {
        opplysninger.add(
            Opplysning(
                label = I18n("Behov for kabin"),
                tekst = I18n(
                    nb = "Bruker har en varig funksjonsnedsettelse som gir kuldeintoleranse, og som fører til at rullestolen ikke kan benyttes uten kabin",
                    nn = "Brukar har ei varig funksjonsnedsetjing som gir kuldeintoleranse, og som fører til at rullestolen ikkje kan nyttast utan kabin",
                ),
            ),
        )

        val kanIkkeAvhjelpesMedEnklereTekst =
            if (hm.elektriskRullestolInfo.kabin.kanIkkeAvhjelpesMedEnklereBegrunnelse != null) {
                Tekst(hm.elektriskRullestolInfo.kabin.kanIkkeAvhjelpesMedEnklereBegrunnelse)
            } else {
                when (hm.elektriskRullestolInfo.kabin.kanIkkeAvhjelpesMedEnklereÅrsak) {
                    KanIkkeAvhjelpesMedEnklereÅrsak.HAR_LUFTVEISPROBLEMER -> Tekst(
                        I18n(
                            nb = "Brukeren har luftveisproblemer og kan ikke bruke varmemaske",
                            nn = "Brukaren har luftvegsproblem og kan ikkje bruka varmemaske",
                        ),
                    )

                    KanIkkeAvhjelpesMedEnklereÅrsak.BEGRENSNING_VED_FUNKSJONSNEDSETTELSE -> Tekst(
                        I18n(
                            nb = "Brukerens funksjonsnedsettelse gjør at hen ikke selv kan benytte varmepose eller ha på seg varme klær",
                            nn = "Funksjonsnedsetjinga til brukaren gjer at hen ikkje sjølv kan nytta varmepose eller ha på seg varme klede",
                        ),
                    )

                    else -> error("elektriskRullestolInfo.kabin burde ha kanIkkeAvhjelpesMedEnklereBegrunnelse når kanIkkeAvhjelpesMedEnklereÅrsak er ANNET. Skal ikke være null når brukerOppfyllerKrav==true ")
                }
            }

        opplysninger.add(
            Opplysning(
                label = I18n(nb = "Enklere løsning er vurdert", nn = "Enklare løysing er vurdert"),
                tekst = kanIkkeAvhjelpesMedEnklereTekst,
            ),
        )
    } else {
        opplysninger.add(
            Opplysning(
                label = I18n("Behov for kabin"),
                tekst = I18n(
                    // TODO her mister vi tekstformatering (italic) rundt ikke/ikkje
                    nb = "Bruker har ikke en varig funksjonsnedsettelse som gir kuldeintoleranse, og som fører til at rullestolen ikke kan benyttes uten kabin",
                    nn = "Brukar har ikkje ei varig funksjonsnedsetjing som gir kuldeintoleranse, og som fører til at rullestolen ikkje kan nyttast utan kabin",
                ),
            ),
        )
        opplysninger.add(
            Opplysning(
                label = I18n("Grunnen til behovet"),
                tekst = hm.elektriskRullestolInfo.kabin.årsakForBehovBegrunnelse
                    ?: error("Mangler hm.elektriskRullestolInfo.kabin.årsakForBehovBegrunnelse"),
            ),
        )
    }
    return opplysninger
}

private fun årsakForAntall(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.årsakForAntall == null) {
        return emptyList()
    }
    return opplysninger(
        label = I18n(nb = "Nødvendig med flere", nn = "Naudsynt med fleire"),
        tekst = if (hm.årsakForAntallBegrunnelse != null) {
            Tekst(hm.årsakForAntallBegrunnelse)
        } else {
            Tekst(
                when (hm.årsakForAntallEnum) {
                    ÅrsakForAntall.BEHOV_I_FLERE_ETASJER -> I18n(
                        nb = "Behov i flere etasjer",
                        nn = "Behov i fleire etasjar",
                    )

                    ÅrsakForAntall.BEHOV_I_FLERE_ROM -> I18n(nb = "Behov i flere rom", nn = "Behov i fleire rom")
                    ÅrsakForAntall.BEHOV_INNENDØRS_OG_UTENDØRS -> I18n(
                        nb = "Behov både innendørs og utendørs",
                        nn = "Behov både innandørs og utandørs",
                    )

                    ÅrsakForAntall.BEHOV_FOR_FLERE_PUTER_FOR_RULLESTOL -> I18n(
                        nb = "Behov for pute til flere rullestoler eller sitteenheter",
                        nn = "Behov for pute til fleire rullestolar eller sitjeeiningar",
                    )

                    ÅrsakForAntall.BEHOV_FOR_JEVNLIG_VASK_ELLER_VEDLIKEHOLD -> I18n(
                        nb = "Behov for jevnlig vask eller vedlikehold",
                        nn = "Behov for jamleg vask eller vedlikehald",
                    )

                    ÅrsakForAntall.BRUKER_HAR_TO_HJEM -> I18n(nb = "Bruker har to hjem", nn = "Bruker har to heimar")
                    ÅrsakForAntall.PUTENE_SKAL_KOMBINERES_I_POSISJONERING -> I18n(
                        nb = "Putene skal kombineres i posisjonering",
                        nn = "Putene skal kombinerast i posisjonering",
                    )

                    ÅrsakForAntall.BEHOV_HJEMME_OG_I_BARNEHAGE -> I18n(
                        nb = "Behov både hjemme og i barnehagen",
                        nn = "Behov både heime og i barnehagen",
                    )

                    ÅrsakForAntall.PUTENE_SKAL_SETTES_SAMMEN_VED_BRUK -> I18n(
                        nb = "Putene skal settes sammen ved bruk",
                        nn = "Putene skal setjast saman ved bruk",
                    )

                    else -> error("Uventet verdi for årsakForAntallEnum: ${hm.årsakForAntallEnum}")
                },
            )
        },
    )
}

private fun rullestolinfo(hm: Hjelpemiddel): List<Opplysning> {
    val opplysninger = mutableListOf<Opplysning>()
    if (hm.rullestolInfo?.skalBrukesIBil == true) {
        opplysninger.add(
            Opplysning(
                label = I18n("Bil"),
                tekst = I18n(
                    nb = "Rullestolen skal brukes som sete i bil",
                    nn = "Rullestolen skal brukast som sete i bil",
                ),
            ),
        )
    }

    if (hm.rullestolInfo?.sitteputeValg != null) {
        opplysninger.add(
            Opplysning(
                label = I18n(nb = "Sittepute", nn = "Sitjepute"),
                tekst = when (hm.rullestolInfo.sitteputeValg) {
                    SitteputeValg.TRENGER_SITTEPUTE -> I18n(
                        nb = "Bruker skal ha sittepute",
                        nn = "Brukar skal ha sitjepute",
                    )

                    SitteputeValg.HAR_FRA_FØR -> I18n(nb = "Har sittepute fra før", nn = "Har sitjepute frå før")
                },
            ),
        )
    }

    return opplysninger
}

private fun tilleggsinformasjon(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.tilleggsinfo.isEmpty()) {
        return emptyList()
    }
    return opplysninger(
        label = I18n("Kommentar"),
        tekst = hm.tilleggsinfo,
    )
}

private fun ersInfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.elektriskRullestolInfo == null) {
        return emptyList()
    }
    val opplysninger = mutableListOf<Opplysning>()
    val betjeneStyringLabel = I18n(nb = "Betjene styring", nn = "Betene styring")

    if (hm.elektriskRullestolInfo.kanBetjeneManuellStyring == true) {
        opplysninger.add(
            Opplysning(
                label = betjeneStyringLabel,
                tekst = I18n(
                    nb = "Brukeren er vurdert til å kunne betjene elektrisk rullestol med manuell styring",
                    nn = "Brukaren er vurdert til å kunne betene elektrisk rullestol med manuell styring",
                ),
            ),
        )
    }

    if (hm.elektriskRullestolInfo.kanBetjeneMotorisertStyring == true) {
        opplysninger.add(
            Opplysning(
                label = betjeneStyringLabel,
                tekst = I18n(
                    nb = "Brukeren er vurdert til å kunne betjene elektrisk rullestol med motorisert styring",
                    nn = "Brukaren er vurdert til å kunne betene elektrisk rullestol med motorisert styring",
                ),
            ),
        )
    }

    return opplysninger
}

private fun tilTekster(i18ns: List<I18n>) = i18ns.map { Tekst(it) }

private fun opplysninger(label: I18n, tekster: List<Tekst>) = listOf(Opplysning(label, tekster))

private fun opplysninger(label: I18n, tekst: Tekst) = listOf(Opplysning(label, tekst))

private fun opplysninger(label: I18n, tekst: I18n) = listOf(Opplysning(label, Tekst(tekst)))

private fun opplysninger(label: I18n, tekst: String) = listOf(Opplysning(label, Tekst(tekst)))
