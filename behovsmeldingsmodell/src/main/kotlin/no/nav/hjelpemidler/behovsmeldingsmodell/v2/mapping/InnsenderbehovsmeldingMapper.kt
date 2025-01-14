package no.nav.hjelpemidler.behovsmeldingsmodell.v2.mapping

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovForSeng
import no.nav.hjelpemidler.behovsmeldingsmodell.Boform
import no.nav.hjelpemidler.behovsmeldingsmodell.BrukersituasjonVilkår
import no.nav.hjelpemidler.behovsmeldingsmodell.BrukersituasjonVilkårtype
import no.nav.hjelpemidler.behovsmeldingsmodell.Bruksarena
import no.nav.hjelpemidler.behovsmeldingsmodell.BruksarenaV2
import no.nav.hjelpemidler.behovsmeldingsmodell.BruksområdeGanghjelpemiddel
import no.nav.hjelpemidler.behovsmeldingsmodell.Funksjonsnedsettelser
import no.nav.hjelpemidler.behovsmeldingsmodell.GanghjelpemiddelType
import no.nav.hjelpemidler.behovsmeldingsmodell.InnsenderRolle
import no.nav.hjelpemidler.behovsmeldingsmodell.KanIkkeAvhjelpesMedEnklereÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.Kontaktperson
import no.nav.hjelpemidler.behovsmeldingsmodell.KontaktpersonV2
import no.nav.hjelpemidler.behovsmeldingsmodell.Oppfølgingsansvarlig
import no.nav.hjelpemidler.behovsmeldingsmodell.OppfølgingsansvarligV2
import no.nav.hjelpemidler.behovsmeldingsmodell.OppreisningsstolBehov
import no.nav.hjelpemidler.behovsmeldingsmodell.OppreisningsstolBruksområde
import no.nav.hjelpemidler.behovsmeldingsmodell.OppreisningsstolLøftType
import no.nav.hjelpemidler.behovsmeldingsmodell.PlasseringType
import no.nav.hjelpemidler.behovsmeldingsmodell.PosisjoneringsputeBehov
import no.nav.hjelpemidler.behovsmeldingsmodell.PosisjoneringsputeForBarnBruk
import no.nav.hjelpemidler.behovsmeldingsmodell.PosisjoneringsputeOppgaverIDagligliv
import no.nav.hjelpemidler.behovsmeldingsmodell.SitteputeValg
import no.nav.hjelpemidler.behovsmeldingsmodell.Utleveringsmåte
import no.nav.hjelpemidler.behovsmeldingsmodell.UtleveringsmåteV2
import no.nav.hjelpemidler.behovsmeldingsmodell.UtlevertType
import no.nav.hjelpemidler.behovsmeldingsmodell.UtlevertTypeV2
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Hjelpemiddel
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Kroppsmål
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Søknad
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Bruker
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Brukersituasjon
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.BrukersituasjonVilkårV2
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.EnkelOpplysning
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.HjelpemiddelProdukt
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Hjelpemidler
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsender
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.InnsenderbehovsmeldingMetadata
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Iso6
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Iso8
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Levering
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.LokalisertTekst
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Opplysning
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Tekst
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Tilbehør
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Utlevertinfo
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Varsel
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Varseltype
import no.nav.hjelpemidler.behovsmeldingsmodell.ÅrsakForAntall

fun tilInnsenderbehovsmeldingV2(v1: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Behovsmelding): Innsenderbehovsmelding {
    val id = v1.id ?: error("Behovsmelding v1 mangler id")
    val v1Bruker = v1.søknad?.bruker ?: error("Behovsmelding $id mangler søknad")
    return Innsenderbehovsmelding(
        id = id,
        type = v1.behovsmeldingType,
        innsendingsdato = v1.søknad.dato ?: error("Behovsmelding $id mangler dato"),
        bruker = Bruker(
            fnr = v1Bruker.fnr,
            navn = v1Bruker.navn,
            signaturtype = v1Bruker.signaturtype ?: error("Behovsmelding $id mangler signaturtype"),
            telefon = v1Bruker.telefon,
            veiadresse = v1Bruker.veiadresse,
            kommunenummer = v1Bruker.kommunenummer,
            brukernummer = v1Bruker.brukernummer,
            kilde = v1Bruker.kilde,
            legacyopplysninger = mutableListOf<EnkelOpplysning>().also {
                if (v1.søknad.brukersituasjon.boform != null) {
                    it.add(
                        EnkelOpplysning(
                            ledetekst = LokalisertTekst(nb = "Boform", nn = "Buform"),
                            innhold = when (v1.søknad.brukersituasjon.boform) {
                                Boform.HJEMME -> LokalisertTekst(nb = "Hjemme", nn = "Heime")
                                Boform.INSTITUSJON -> LokalisertTekst("Institusjon")
                                Boform.HJEMME_I_EGEN_BOLIG -> LokalisertTekst(
                                    nb = "Hjemme i egen bolig",
                                    nn = "Heime i eigen bustad",
                                )

                                Boform.HJEMME_I_EGEN_BOLIG_OMSORGSBOLIG_BOFELLESSKAP_SERVICEBOLIG -> LokalisertTekst(
                                    nb = "Hjemme i omsorgsbolig, bofellesskap eller servicebolig",
                                    nn = "Heime i omsorgsbustad, bufellesskap eller servicebustad",
                                )
                            },
                        ),
                    )
                }
                if (v1.søknad.brukersituasjon.bruksarenaErDagliglivet == true) {
                    it.add(
                        EnkelOpplysning(
                            ledetekst = LokalisertTekst("Bruksarena"),
                            innhold = LokalisertTekst(nb = "Dagliglivet", nn = "Dagleglivet"),
                        ),
                    )
                }
            },
        ),
        brukersituasjon = Brukersituasjon(
            vilkår = tilBrukersituasjonVilkårV2(v1.søknad),
            funksjonsnedsettelser = mutableSetOf<Funksjonsnedsettelser>().also {
                if (v1.søknad.brukersituasjon.funksjonsnedsettelser.bevegelse) {
                    it.add(Funksjonsnedsettelser.BEVEGELSE)
                }
                if (v1.søknad.brukersituasjon.funksjonsnedsettelser.kognisjon) {
                    it.add(Funksjonsnedsettelser.KOGNISJON)
                }
                if (v1.søknad.brukersituasjon.funksjonsnedsettelser.hørsel) {
                    it.add(Funksjonsnedsettelser.HØRSEL)
                }
            },
            funksjonsbeskrivelse = v1.søknad.brukersituasjon.funksjonsbeskrivelse,
        ),
        levering = Levering(
            hjelpemiddelformidler = v1.søknad.levering.hjelpemiddelformidler,
            oppfølgingsansvarlig = when (v1.søknad.levering.oppfølgingsansvarlig) {
                Oppfølgingsansvarlig.HJELPEMIDDELFORMIDLER -> OppfølgingsansvarligV2.HJELPEMIDDELFORMIDLER
                Oppfølgingsansvarlig.ANNEN_OPPFØLGINGSANSVARLIG -> OppfølgingsansvarligV2.ANNEN_OPPFØLGINGSANSVARLIG
                null -> OppfølgingsansvarligV2.HJELPEMIDDELFORMIDLER
            },
            annenOppfølgingsansvarlig = v1.søknad.levering.annenOppfølgingsansvarlig,
            utleveringsmåte = when (v1.søknad.levering.utleveringsmåte) {
                Utleveringsmåte.FOLKEREGISTRERT_ADRESSE -> UtleveringsmåteV2.FOLKEREGISTRERT_ADRESSE
                Utleveringsmåte.ANNEN_BRUKSADRESSE -> UtleveringsmåteV2.ANNEN_BRUKSADRESSE
                Utleveringsmåte.HJELPEMIDDELSENTRALEN -> UtleveringsmåteV2.HJELPEMIDDELSENTRALEN
                Utleveringsmåte.ALLEREDE_UTLEVERT_AV_NAV -> UtleveringsmåteV2.ALLEREDE_UTLEVERT_AV_NAV
                null -> null
            },
            annenUtleveringsadresse = v1.søknad.levering.annenUtleveringsadresse,
            utleveringKontaktperson = when (v1.søknad.levering.utleveringKontaktperson) {
                Kontaktperson.HJELPEMIDDELBRUKER -> KontaktpersonV2.HJELPEMIDDELBRUKER
                Kontaktperson.HJELPEMIDDELFORMIDLER -> KontaktpersonV2.HJELPEMIDDELFORMIDLER
                Kontaktperson.ANNEN_KONTAKTPERSON -> KontaktpersonV2.ANNEN_KONTAKTPERSON
                null -> null
            },
            annenKontaktperson = v1.søknad.levering.annenKontaktperson,
            utleveringMerknad = v1.søknad.levering.utleveringMerknad,
            hast = v1.søknad.hast,
            automatiskUtledetTilleggsinfo = v1.søknad.levering.tilleggsinfo,

        ),
        innsender = Innsender(
            rolle = v1.søknad.innsender?.somRolle ?: InnsenderRolle.FORMIDLER,
            erKommunaltAnsatt = v1.søknad.innsender?.erKommunaltAnsatt,
            kurs = v1.søknad.innsender?.godkjenningskurs ?: emptyList(),
            sjekketUtlånsoversiktForKategorier = v1.søknad.innsender?.tjenestligeBehovForUtlånsoversikt?.map { Iso6(it) }
                ?.toSet() ?: emptySet(),
        ),
        hjelpemidler = Hjelpemidler(
            totaltAntall = v1.søknad.hjelpemidler.totaltAntall,
            hjelpemidler = v1.søknad.hjelpemidler.hjelpemidler.map { tilHjelpemiddelV2(it, v1.søknad) },
        ),
        metadata = InnsenderbehovsmeldingMetadata(
            bestillingsordningsjekk = v1.bestillingsordningsjekk,
        ),
    )
}

fun tilBrukersituasjonVilkårV2(v1: Søknad): Set<BrukersituasjonVilkårV2> {
    val innbyggernavn = v1.bruker.navn.toString()
    val innsendernavn = v1.levering.hjelpemiddelformidler.navn.toString()
    fun nedsattFunksjonTekst() = LokalisertTekst(
        nb = "$innbyggernavn har vesentlig og varig nedsatt funksjonsevne som følge av sykdom, skade eller lyte. Med varig menes 2 år eller livet ut.",
        nn = "$innbyggernavn har vesentleg og varig nedsett funksjonsevne som følgje av sjukdom, skade eller lyte. Med varig siktar ein til 2 år eller livet ut.",
    )

    fun størreBehovTekst() = LokalisertTekst(
        nb = "Hjelpemiddelet(ene) er nødvendig for å avhjelpe praktiske problemer i dagliglivet eller bli pleid i hjemmet. Brukers behov kan ikke løses med enklere og rimeligere hjelpemidler eller ved andre tiltak som ikke dekkes av Nav.",
        nn = "Hjelpemiddelet(a) er naudsynt for å avhjelpa praktiske problem i dagleglivet eller bli pleidd i heimen. Brukars behov kan ikkje løysast med enklare og rimelegare hjelpemiddel eller ved andre tiltak som ikkje blir dekt av Nav.",
    )

    fun praktiskeProblemTekst() = LokalisertTekst(
        nb = "Hjelpemiddelet(ene) er egnet til å avhjelpe funksjonsnedsettelsen og $innbyggernavn vil være i stand til å bruke det.",
        nn = "Hjelpemiddelet(a) er eigna til å avhjelpa funksjonsnedsetjinga og $innbyggernavn vil vera i stand til å bruka det.",
    )

    fun praktiskeProblemerIDagliglivetTekst() = LokalisertTekst(
        nb = "Hjelpemiddelet er nødvendig for å avhjelpe praktiske problemer i dagliglivet, eller for å bli pleid i hjemmet.",
        nn = "Hjelpemiddelet er naudsynt for å avhjelpa praktiske problem i dagleglivet, eller for å bli pleidd i heimen.",
    )

    fun vesentligOgVarigNedsattFunksjonsevneTekst() = LokalisertTekst(
        nb = "$innbyggernavn har vesentlig og varig nedsatt funksjonsevne som følge av sykdom, skade eller lyte. Med varig menes 2 år eller livet ut. Hjelpemiddelet skal ikke brukes til korttidsutlån eller til andre formål.",
        nn = "$innbyggernavn har vesentleg og varig nedsett funksjonsevne som følgje av sjukdom, skade eller lyte. Med varig siktar ein til 2 år eller livet ut. Hjelpemiddelet skal ikkje brukast til korttidsutlån eller til andre formål.",
    )

    fun kanIkkeLøsesMedEnklereHjelpemidlerTekst() = LokalisertTekst(
        nb = "$innbyggernavn sitt behov kan ikke løses med enklere og rimeligere hjelpemidler, eller ved andre tiltak som ikke dekkes av Nav.",
        nn = "$innbyggernavn sitt behov kan ikkje løysast med enklare og rimelegare hjelpemiddel, eller ved andre tiltak som ikkje blir dekt av Nav.",
    )

    fun iStandTilÅBrukeHjelpemidleneTekst() = LokalisertTekst(
        nb = "$innbyggernavn vil være i stand til å bruke hjelpemidlene. $innsendernavn har ansvaret for at hjelpemidlene blir levert, og at nødvendig opplæring, tilpasning og montering blir gjort.",
        nn = "$innbyggernavn vil vera i stand til å bruka hjelpemidla. $innsendernavn har ansvaret for at hjelpemidla blir leverte, og at nødvendig opplæring, tilpassing og montering blir gjord.",
    )

    return v1.brukersituasjon.bekreftedeVilkår.map { vilkår ->
        when (vilkår) {
            BrukersituasjonVilkår.NEDSATT_FUNKSJON -> BrukersituasjonVilkårV2(
                BrukersituasjonVilkårtype.NEDSATT_FUNKSJON,
                nedsattFunksjonTekst(),
            )

            BrukersituasjonVilkår.STØRRE_BEHOV -> BrukersituasjonVilkårV2(
                BrukersituasjonVilkårtype.STØRRE_BEHOV,
                størreBehovTekst(),
            )

            BrukersituasjonVilkår.PRAKTISKE_PROBLEM -> BrukersituasjonVilkårV2(
                BrukersituasjonVilkårtype.PRAKTISKE_PROBLEM,
                praktiskeProblemTekst(),
            )

            BrukersituasjonVilkår.PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1 -> BrukersituasjonVilkårV2(
                BrukersituasjonVilkårtype.PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1,
                praktiskeProblemerIDagliglivetTekst(),
            )

            BrukersituasjonVilkår.VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1 -> BrukersituasjonVilkårV2(
                BrukersituasjonVilkårtype.VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1,
                vesentligOgVarigNedsattFunksjonsevneTekst(),
            )

            BrukersituasjonVilkår.KAN_IKKE_LØSES_MED_ENKLERE_HJELPEMIDLER_V1 -> BrukersituasjonVilkårV2(
                BrukersituasjonVilkårtype.KAN_IKKE_LØSES_MED_ENKLERE_HJELPEMIDLER_V1,
                kanIkkeLøsesMedEnklereHjelpemidlerTekst(),
            )

            BrukersituasjonVilkår.I_STAND_TIL_Å_BRUKE_HJELPEMIDLENE_V1 -> BrukersituasjonVilkårV2(
                BrukersituasjonVilkårtype.I_STAND_TIL_Å_BRUKE_HJELPEMIDLENE_V1,
                iStandTilÅBrukeHjelpemidleneTekst(),
            )
        }
    }.toMutableSet().also {
        if (it.isEmpty()) {
            // Håndter eldre variant av datamodellen
            if (v1.brukersituasjon.skalIkkeBrukesTilAndreFormål == true) {
                // Bestilling
                it.add(
                    BrukersituasjonVilkårV2(
                        BrukersituasjonVilkårtype.VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1,
                        vesentligOgVarigNedsattFunksjonsevneTekst(),
                    ),
                )
                if (v1.brukersituasjon.størreBehov == true) {
                    it.add(
                        BrukersituasjonVilkårV2(
                            BrukersituasjonVilkårtype.KAN_IKKE_LØSES_MED_ENKLERE_HJELPEMIDLER_V1,
                            kanIkkeLøsesMedEnklereHjelpemidlerTekst(),
                        ),
                    )
                }
                if (v1.brukersituasjon.praktiskeProblem == true) {
                    it.add(
                        BrukersituasjonVilkårV2(
                            BrukersituasjonVilkårtype.I_STAND_TIL_Å_BRUKE_HJELPEMIDLENE_V1,
                            iStandTilÅBrukeHjelpemidleneTekst(),
                        ),
                    )
                }
                if (v1.brukersituasjon.bruksarenaErDagliglivet == true) {
                    it.add(
                        BrukersituasjonVilkårV2(
                            BrukersituasjonVilkårtype.PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1,
                            praktiskeProblemerIDagliglivetTekst(),
                        ),
                    )
                }
            } else {
                // Formidlersøknad
                if (v1.brukersituasjon.nedsattFunksjon == true) {
                    it.add(BrukersituasjonVilkårV2(BrukersituasjonVilkårtype.NEDSATT_FUNKSJON, nedsattFunksjonTekst()))
                }
                if (v1.brukersituasjon.størreBehov == true) {
                    it.add(BrukersituasjonVilkårV2(BrukersituasjonVilkårtype.STØRRE_BEHOV, størreBehovTekst()))
                }
                if (v1.brukersituasjon.praktiskeProblem == true) {
                    it.add(
                        BrukersituasjonVilkårV2(
                            BrukersituasjonVilkårtype.PRAKTISKE_PROBLEM,
                            praktiskeProblemTekst(),
                        ),
                    )
                }
            }
        }
    }
}

fun tilHjelpemiddelV2(v1: Hjelpemiddel, søknad: Søknad): no.nav.hjelpemidler.behovsmeldingsmodell.v2.Hjelpemiddel {
    val id = søknad.id
    return no.nav.hjelpemidler.behovsmeldingsmodell.v2.Hjelpemiddel(
        hjelpemiddelId = v1.uniqueKey,
        antall = v1.antall,
        produkt = HjelpemiddelProdukt(
            hmsArtNr = v1.hmsnr,
            artikkelnavn = v1.beskrivelse,
            iso8 = padIso8(v1.produkt?.isocode) ?: error("Behovsmelding $id mangler isocode for ${v1.hmsnr}"),
            iso8Tittel = v1.produkt?.isotitle ?: error("Behovsmelding $id mangler isotitle for ${v1.hmsnr}"),
            rangering = parseRangering(v1.produkt.postrank),
            delkontrakttittel = v1.produkt.aposttitle
                ?: error("Behovsmelding $id mangler delkontrakttittel for ${v1.hmsnr}"),
            sortimentkategori = v1.produkt.kategori ?: error("v1.produkt.kategori (sortimentkategori) mangler"),
            delkontraktId = v1.produkt.apostid,
        ),
        tilbehør = (v1.tilbehør ?: emptyList()).map {
            Tilbehør(
                hmsArtNr = it.hmsnr,
                navn = it.navn,
                antall = it.antall!!,
                begrunnelse = it.begrunnelse,
                fritakFraBegrunnelseÅrsak = it.fritakFraBegrunnelseÅrsak,
            )
        },
        bytter = v1.bytter,
        bruksarenaer = v1.bruksarena.map { bruksarena ->
            when (bruksarena) {
                Bruksarena.EGET_HJEM -> BruksarenaV2.EGET_HJEM
                Bruksarena.EGET_HJEM_IKKE_AVLASTNING -> BruksarenaV2.EGET_HJEM_IKKE_AVLASTNING
                Bruksarena.OMSORGSBOLIG_BOFELLESKAP_SERVICEBOLIG -> BruksarenaV2.OMSORGSBOLIG_BOFELLESKAP_SERVICEBOLIG
                Bruksarena.BARNEHAGE -> BruksarenaV2.BARNEHAGE
                Bruksarena.GRUNN_ELLER_VIDEREGÅENDE_SKOLE -> BruksarenaV2.GRUNN_ELLER_VIDEREGÅENDE_SKOLE
                Bruksarena.SKOLEFRITIDSORDNING -> BruksarenaV2.SKOLEFRITIDSORDNING
                Bruksarena.INSTITUSJON -> BruksarenaV2.INSTITUSJON
                Bruksarena.INSTITUSJON_BARNEBOLIG -> BruksarenaV2.INSTITUSJON_BARNEBOLIG
                Bruksarena.INSTITUSJON_BARNEBOLIG_KUN_PERSONLIG_BRUK -> BruksarenaV2.INSTITUSJON_BARNEBOLIG_KUN_PERSONLIG_BRUK
            }
        }.toSet(),
        utlevertinfo = Utlevertinfo(
            alleredeUtlevertFraHjelpemiddelsentralen = v1.utlevertFraHjelpemiddelsentralen,
            utleverttype = when (v1.utlevertInfo?.utlevertType) {
                UtlevertType.FREMSKUTT_LAGER -> UtlevertTypeV2.FREMSKUTT_LAGER
                UtlevertType.KORTTIDSLÅN -> UtlevertTypeV2.KORTTIDSLÅN
                UtlevertType.OVERFØRT -> UtlevertTypeV2.OVERFØRT
                UtlevertType.ANNET -> UtlevertTypeV2.ANNET
                null -> null
            },
            overførtFraBruker = v1.utlevertInfo?.overførtFraBruker,
            annenKommentar = v1.utlevertInfo?.annenKommentar,
        ),
        opplysninger = opplysninger(v1, søknad),
        varsler = varsler(v1),
    )
}

fun opplysninger(hm: Hjelpemiddel, søknad: Søknad): List<Opplysning> = listOf(
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
    kroppsmål(hm, søknad.bruker.kroppsmål),
    seilEllerSele(hm),
    appinfo(hm),
    varmehjelpemiddelinfo(hm),
    sengeinfo(hm),
    elektriskVendesystemInfo(hm),
    posisjoneringssysteminfo(hm),
    posisjoneringsputerForBarnInfo(hm),
    diverseinfo(hm),
    oppreisningsstolInfo(hm),
    ganghjelpemiddelInfo(hm),
).flatten()

fun varsler(hm: Hjelpemiddel): List<Varsel> {
    val varsler = mutableListOf<Varsel>()

    val over26År = LokalisertTekst("Personen er over 26 år.")

    if (hm.elektriskVendesystemInfo?.standardLakenByttesTilRiktigStørrelseAvNav == true &&
        hm.elektriskVendesystemInfo.sengForMontering?.madrassbredde != null
    ) {
        val bredde: Int = hm.elektriskVendesystemInfo.sengForMontering.madrassbredde
        varsler.add(
            Varsel(
                LokalisertTekst(
                    nb = "Standard glidelaken og trekklaken byttes av Nav til å passe $bredde cm bredde.",
                    nn = "Standard glidelaken og trekklaken blir bytt av Nav til å passa $bredde cm breidd.",
                ),
                Varseltype.INFO,
            ),
        )
    }

    if (hm.sengInfo?.høyGrindValg?.erLagetPlanForOppfølging == false) {
        varsler.add(
            Varsel(
                LokalisertTekst(
                    nb = "Før hjelpemiddelsentralen kan behandle saken må det være laget en plan for hjelpemiddelbruken. Nav må innhente opplysninger.",
                    nn = "Før hjelpemiddelsentralen kan behandla saka må det vera laga ein plan for hjelpemiddelbruken. Nav må innhenta opplysningar.",
                ),
                Varseltype.INFO,
            ),
        )
    }

    if (hm.posisjoneringsputeForBarnInfo?.brukerErOver26År == true) {
        varsler.add(Varsel(over26År, Varseltype.WARNING))
    }

    if (hm.ganghjelpemiddelInfo?.brukerErFylt26År == true && hm.ganghjelpemiddelInfo.type == GanghjelpemiddelType.GÅTRENING) {
        varsler.add(Varsel(over26År, Varseltype.WARNING))
    }

    if (hm.produkt?.påkrevdGodkjenningskurs != null) {
        val kurs =
            hm.produkt.påkrevdGodkjenningskurs.tittel?.lowercase() ?: error("påkrevdGodkjenningskurs.tittel er null")
        val erVerifisert =
            hm.produkt.påkrevdGodkjenningskurs.formidlersGjennomføring == no.nav.hjelpemidler.behovsmeldingsmodell.HjelpemiddelProdukt.FormidlersGjennomføringAvKurs.GODKJENNINGSKURS_DB
        val erERS =
            hm.produkt.påkrevdGodkjenningskurs.kursId == no.nav.hjelpemidler.behovsmeldingsmodell.HjelpemiddelProdukt.KursId.ELEKTRISK_RULLESTOL.id

        if (!erVerifisert) {
            if (erERS) {
                varsler.add(
                    Varsel(
                        LokalisertTekst(
                            nb = "Kommunal formidler har svart at godkjenningskurs $kurs (del 1 og del 2) er gjennomført. Dokumentasjon av kurs sjekkes i behandling av saken.",
                            nn = "Kommunal formidlar har svart at godkjenningskurs $kurs (del 1 og del 2) er gjennomført. Dokumentasjon av kurs blir sjekka i behandling av saka.",
                        ),
                        Varseltype.WARNING,
                    ),
                )
            } else {
                varsler.add(
                    Varsel(
                        LokalisertTekst(
                            nb = "Kommunal formidler har svart at godkjenningskurs $kurs er gjennomført. Dokumentasjon av kurs sjekkes i behandling av saken.",
                            nn = "Kommunal formidlar har svart at godkjenningskurs $kurs er gjennomført. Dokumentasjon av kurs blir sjekka i behandling av saka.",
                        ),
                        Varseltype.WARNING,
                    ),
                )
            }
        }
    }

    return varsler
}

private fun utlevertinfo(hm: Hjelpemiddel): List<Opplysning> {
    if (!hm.utlevertFraHjelpemiddelsentralen || hm.utlevertInfo?.utlevertType == null) {
        return emptyList()
    }
    val brukernummer = hm.utlevertInfo.overførtFraBruker
    return opplysninger(
        ledetekst = LokalisertTekst("Utlevert"),
        tekst = when (hm.utlevertInfo.utlevertType) {
            UtlevertType.FREMSKUTT_LAGER -> Tekst(
                nb = "Utlevert fra fremskutt lager",
                nn = "Utlevert frå framskote lager",
            )

            UtlevertType.KORTTIDSLÅN -> Tekst(
                LokalisertTekst("Utprøvingslån"),
            )

            UtlevertType.OVERFØRT -> Tekst(
                nb = "Overført fra annen bruker. Brukernummer: $brukernummer",
                nn = "Overført frå annan brukar. Brukarnummer: $brukernummer",
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
        ledetekst = LokalisertTekst("Bruksarena"),
        tekster =
        hm.bruksarena.map {
            when (it) {
                Bruksarena.EGET_HJEM -> Tekst(
                    nb = "I eget hjem.",
                    nn = "I eigen heim.",
                )

                Bruksarena.EGET_HJEM_IKKE_AVLASTNING -> Tekst(
                    forhåndsdefinertTekst = LokalisertTekst(
                        nb = "I eget hjem. Ikke avlastningsbolig.",
                        nn = "I eigen heim. Ikkje avlastingsbustad.",
                    ),
                    begrepsforklaring = LokalisertTekst(
                        nb = "Med avlastningsbolig menes en tjeneste som kommunen betaler for. Det kan være privat eller kommunalt. Det er kommunens ansvar å dekke hjelpemidler i avlastningsbolig.",
                        nn = "Med avlastingsbustad siktar ein til ei teneste som kommunen betaler for. Det kan vere privat eller kommunalt. Det er ansvaret til kommunen å dekkje hjelpemiddel i avlastingsbustad.",
                    ),
                )

                Bruksarena.OMSORGSBOLIG_BOFELLESKAP_SERVICEBOLIG -> Tekst(
                    nb = "I omsorgsbolig, bofellesskap eller servicebolig.",
                    nn = "I omsorgsbustad, bufellesskap eller servicebustad.",
                )

                Bruksarena.BARNEHAGE -> Tekst(LokalisertTekst("I barnehage."))

                Bruksarena.GRUNN_ELLER_VIDEREGÅENDE_SKOLE -> Tekst(
                    nb = "På skolen som grunnskole eller videregående skole.",
                    nn = "På skulen som grunnskule eller vidaregåande skule.",
                )

                Bruksarena.SKOLEFRITIDSORDNING -> Tekst(
                    nb = "På skolefritidsordning.",
                    nn = "På skulefritidsordning.",
                )

                Bruksarena.INSTITUSJON -> Tekst(
                    nb = "På institusjon som sykehjem.",
                    nn = "På institusjon som sjukeheim.",
                )

                Bruksarena.INSTITUSJON_BARNEBOLIG -> Tekst(
                    nb = "På institusjon som sykehjem eller barnebolig.",
                    nn = "På institusjon som sjukeheim eller barnebustad.",
                )

                Bruksarena.INSTITUSJON_BARNEBOLIG_KUN_PERSONLIG_BRUK -> Tekst(
                    nb = "På institusjon som sykehjem eller barnebolig, og hjelpemiddelet skal kun være til personlig bruk.",
                    nn = "På institusjon som sjukeheim eller barnebustad, og hjelpemiddelet skal berre vera til personlig bruk.",
                )
            }
        },
    )
}

private fun trykksårforebygging(hm: Hjelpemiddel): List<Opplysning> {
    val valgteVilkår = hm.vilkår?.filter { it.avhuket }
    if (valgteVilkår.isNullOrEmpty()) {
        return emptyList()
    }
    return opplysninger(
        ledetekst = behov,
        tekster = valgteVilkår.map {
            if (it.tilleggsinfo != null) {
                Tekst(fritekst = it.tilleggsinfo)
            } else {
                Tekst(forhåndsdefinertTekst = LokalisertTekst(it.vilkårstekst))
            }
        },
    )
}

private fun begrunnelseLavereRangeringEllerIkkeTilsvarende(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.begrunnelse.isNullOrEmpty()) {
        return emptyList()
    }
    val label = if (hm.kanIkkeTilsvarende == true) {
        val rangering = parseRangering(hm.produkt?.postrank)
        if (rangering != null && rangering > 1) {
            LokalisertTekst(nb = "Begrunnelse for lavere rangering", nn = "Grunngiving for lågare rangering")
        } else {
            LokalisertTekst(nb = "Kan ikke ha tilsvarende fordi", nn = "Kan ikkje ha tilsvarande fordi")
        }
    } else {
        LokalisertTekst(nb = "Begrunnelse", nn = "Grunngiving")
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
    return if (erVerifisert) {
        opplysninger(
            ledetekst = LokalisertTekst("Krav om kurs"),
            tekst =
            if (erERS) {
                Tekst(
                    nb = "Det er dokumentert at innsender har fullført og bestått både del 1 (teoretisk) og del 2 (praktisk) av godkjenningskurs $kurs.",
                    nn = "Det er dokumentert at innsendar har fullført og bestått både del 1 (teoretisk) og del 2 (praktisk) av godkjenningskurs $kurs.",
                )
            } else {
                Tekst(
                    nb = "Det er dokumentert at innsender har fullført og bestått godkjenningskurs $kurs.",
                    nn = "Det er dokumentert at innsendar har fullført og bestått godkjenningskurs $kurs.",
                )
            },
        )
    } else {
        emptyList() // blir lagt inn som varsel i stedet
    }
}

private fun ersMedKabin(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.elektriskRullestolInfo?.kabin == null) {
        return emptyList()
    }
    val opplysninger = mutableListOf<Opplysning>()
    if (hm.elektriskRullestolInfo.kabin.brukerOppfyllerKrav) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Behov for kabin"),
                innhold = Tekst(
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
                        nb = "Brukeren har luftveisproblemer og kan ikke bruke varmemaske",
                        nn = "Brukaren har luftvegsproblem og kan ikkje bruka varmemaske",
                    )

                    KanIkkeAvhjelpesMedEnklereÅrsak.BEGRENSNING_VED_FUNKSJONSNEDSETTELSE -> Tekst(
                        nb = "Brukerens funksjonsnedsettelse gjør at hen ikke selv kan benytte varmepose eller ha på seg varme klær",
                        nn = "Funksjonsnedsetjinga til brukaren gjer at hen ikkje sjølv kan nytta varmepose eller ha på seg varme klede",
                    )

                    else -> error("elektriskRullestolInfo.kabin burde ha kanIkkeAvhjelpesMedEnklereBegrunnelse når kanIkkeAvhjelpesMedEnklereÅrsak er ANNET. Skal ikke være null når brukerOppfyllerKrav==true ")
                }
            }

        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst(nb = "Enklere løsning er vurdert", nn = "Enklare løysing er vurdert"),
                innhold = kanIkkeAvhjelpesMedEnklereTekst,
            ),
        )
    } else {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Behov for kabin"),
                innhold = Tekst(
                    nb = "Bruker har <em>ikke</em> en varig funksjonsnedsettelse som gir kuldeintoleranse, og som fører til at rullestolen ikke kan benyttes uten kabin",
                    nn = "Brukar har <em>ikkje</em> ei varig funksjonsnedsetjing som gir kuldeintoleranse, og som fører til at rullestolen ikkje kan nyttast utan kabin",
                ),
            ),
        )
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Grunnen til behovet"),
                innhold = hm.elektriskRullestolInfo.kabin.årsakForBehovBegrunnelse
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
        ledetekst = LokalisertTekst(nb = "Nødvendig med flere", nn = "Naudsynt med fleire"),
        tekst = if (hm.årsakForAntallBegrunnelse != null) {
            Tekst(hm.årsakForAntallBegrunnelse)
        } else {
            when (hm.årsakForAntallEnum) {
                ÅrsakForAntall.BEHOV_I_FLERE_ETASJER -> Tekst(
                    nb = "Behov i flere etasjer",
                    nn = "Behov i fleire etasjar",
                )

                ÅrsakForAntall.BEHOV_I_FLERE_ROM -> Tekst(
                    nb = "Behov i flere rom",
                    nn = "Behov i fleire rom",
                )

                ÅrsakForAntall.BEHOV_INNENDØRS_OG_UTENDØRS -> Tekst(
                    nb = "Behov både innendørs og utendørs",
                    nn = "Behov både innandørs og utandørs",
                )

                ÅrsakForAntall.BEHOV_FOR_FLERE_PUTER_FOR_RULLESTOL -> Tekst(
                    nb = "Behov for pute til flere rullestoler eller sitteenheter",
                    nn = "Behov for pute til fleire rullestolar eller sitjeeiningar",
                )

                ÅrsakForAntall.BEHOV_FOR_JEVNLIG_VASK_ELLER_VEDLIKEHOLD -> Tekst(
                    nb = "Behov for jevnlig vask eller vedlikehold",
                    nn = "Behov for jamleg vask eller vedlikehald",
                )

                ÅrsakForAntall.BRUKER_HAR_TO_HJEM -> Tekst(
                    nb = "Bruker har to hjem",
                    nn = "Bruker har to heimar",
                )

                ÅrsakForAntall.PUTENE_SKAL_KOMBINERES_I_POSISJONERING -> Tekst(
                    nb = "Putene skal kombineres i posisjonering",
                    nn = "Putene skal kombinerast i posisjonering",
                )

                ÅrsakForAntall.BEHOV_HJEMME_OG_I_BARNEHAGE -> Tekst(
                    nb = "Behov både hjemme og i barnehagen",
                    nn = "Behov både heime og i barnehagen",
                )

                ÅrsakForAntall.PUTENE_SKAL_SETTES_SAMMEN_VED_BRUK -> Tekst(
                    nb = "Putene skal settes sammen ved bruk",
                    nn = "Putene skal setjast saman ved bruk",
                )

                else -> error("Uventet verdi for årsakForAntallEnum: ${hm.årsakForAntallEnum}")
            }
        },
    )
}

private fun rullestolinfo(hm: Hjelpemiddel): List<Opplysning> {
    val opplysninger = mutableListOf<Opplysning>()
    if (hm.rullestolInfo?.skalBrukesIBil == true) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Bil"),
                innhold = Tekst(
                    nb = "Rullestolen skal brukes som sete i bil",
                    nn = "Rullestolen skal brukast som sete i bil",
                ),
            ),
        )
    }

    if (hm.rullestolInfo?.sitteputeValg != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst(nb = "Sittepute", nn = "Sitjepute"),
                innhold = when (hm.rullestolInfo.sitteputeValg) {
                    SitteputeValg.TRENGER_SITTEPUTE -> Tekst(
                        nb = "Bruker skal ha sittepute",
                        nn = "Brukar skal ha sitjepute",
                    )

                    SitteputeValg.HAR_FRA_FØR -> Tekst(
                        nb = "Har sittepute fra før",
                        nn = "Har sitjepute frå før",
                    )

                    SitteputeValg.STANDARD_SITTEPUTE -> Tekst(
                        nb = "Ønsker standard sittepute",
                        nn = "Ynskjer standard sitjepute",
                    )

                    SitteputeValg.LEGGES_TIL_SEPARAT -> Tekst(
                        nb = "Trykkavlastende sittepute legges til separat",
                        nn = "Trykkavlastande sitjepute leggjast til separat",
                    )
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
        ledetekst = LokalisertTekst("Kommentar"),
        tekst = hm.tilleggsinfo,
    )
}

private fun ersInfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.elektriskRullestolInfo == null) {
        return emptyList()
    }
    val opplysninger = mutableListOf<Opplysning>()
    val betjeneStyringLabel = LokalisertTekst(nb = "Betjene styring", nn = "Betene styring")

    if (hm.elektriskRullestolInfo.kanBetjeneManuellStyring == true) {
        opplysninger.add(
            Opplysning(
                ledetekst = betjeneStyringLabel,
                innhold = Tekst(
                    nb = "Brukeren er vurdert til å kunne betjene elektrisk rullestol med manuell styring",
                    nn = "Brukaren er vurdert til å kunne betene elektrisk rullestol med manuell styring",
                ),
            ),
        )
    }

    if (hm.elektriskRullestolInfo.kanBetjeneMotorisertStyring == true) {
        opplysninger.add(
            Opplysning(
                ledetekst = betjeneStyringLabel,
                innhold = Tekst(
                    nb = "Brukeren er vurdert til å kunne betjene elektrisk rullestol med motorisert styring",
                    nn = "Brukaren er vurdert til å kunne betene elektrisk rullestol med motorisert styring",
                ),
            ),
        )
    }

    if (hm.elektriskRullestolInfo.ferdesSikkertITrafikk == true) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Trafikk"),
                innhold = Tekst(
                    nb = "Brukeren er vurdert til å kunne ferdes sikkert i trafikken",
                    nn = "Brukaren er vurdert til å kunne ferdast sikkert i trafikken",
                ),
            ),
        )
    }

    if (hm.elektriskRullestolInfo.nedsattGangfunksjon == true) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Nedsatt gangfunksjon"),
                innhold = Tekst(
                    nb = "Brukeren skal benytte den elektriske rullestolen til å avhjelpe en vesentlig nedsatt gangfunksjon. Den skal ikke brukes til et generelt transportbehov.",
                    nn = "Brukaren skal benytte den elektriske rullestolen til å avhjelpe ein vesentlig nedsatt gangfunksjon. Den skal ikkje brukes til eit generelt transportbehov.",
                ),
            ),
        )
    }

    if (hm.elektriskRullestolInfo.oppbevaringOgLading != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Oppbevaring og lading"),
                innhold = when (hm.elektriskRullestolInfo.oppbevaringOgLading) {
                    true -> Tekst(
                        nb = "Bruker har egnet sted for oppbevaring og lading",
                        nn = "Brukar har eigna sted for oppbevaring og lading",
                    )

                    false -> Tekst(
                        hm.elektriskRullestolInfo.oppbevaringInfo
                            ?: error("elektriskRullestolInfo.oppbevaringInfo mangler"),
                    )
                },

            ),
        )
    }

    if (hm.elektriskRullestolInfo.kjentMedForsikring != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Forsikringsvilkår"),
                innhold = when (hm.elektriskRullestolInfo.kjentMedForsikring) {
                    true -> Tekst(
                        nb = "Bruker er gjort kjent med forsikringsvilkårene",
                        nn = "Brukar er gjort kjent med forsikringsvilkåra",
                    )

                    false -> Tekst(
                        nb = "Bruker gjøres kjent med forsikringsvilkårene i forbindelse med opplæringen",
                        nn = "Brukar blir gjorde kjent med forsikringsvilkåra i samband med opplæringa",
                    )
                },

            ),
        )
    }

    if (hm.elektriskRullestolInfo.harSpesialsykkel != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Spesialsykkel"),
                innhold = when (hm.elektriskRullestolInfo.harSpesialsykkel) {
                    true -> Tekst(
                        nb = "Bruker har spesialsykkel fra før",
                        nn = "Brukar har spesialsykkel frå før",
                    )

                    false -> Tekst(
                        nb = "Bruker har ikke spesialsykkel fra før",
                        nn = "Brukar har ikkje spesialsykkel frå før",
                    )
                },
            ),
        )
    }

    if (hm.elektriskRullestolInfo.plasseringAvHendel != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Gasshendel"),
                innhold = when (hm.elektriskRullestolInfo.plasseringAvHendel) {
                    PlasseringType.HØYRE -> Tekst(
                        nb = "Skal plasseres på høyre side",
                        nn = "Skal plasserast på høgre side",
                    )

                    PlasseringType.VENSTRE -> Tekst(
                        nb = "Skal plasseres på venstre side",
                        nn = "Skal plasserast på venstre side",
                    )
                },
            ),
        )
    }

    return opplysninger
}

fun kroppsmål(hm: Hjelpemiddel, kroppsmål: Kroppsmål?): List<Opplysning> {
    if (kroppsmål == null) {
        return emptyList()
    }
    if (hm.produkt?.kategori !in setOf(
            "Manuelle rullestoler",
            "Elektriske rullestoler",
            "Stoler med oppreisingsfunksjon",
        )
    ) {
        return emptyList()
    }

    return opplysninger(
        ledetekst = LokalisertTekst("Kroppsmål"),
        tekst = with(kroppsmål) {
            Tekst(
                nb = "Setebredde: $setebredde cm, lårlengde: $lårlengde cm, legglengde: $legglengde cm, høyde: $høyde cm, kroppsvekt: $kroppsvekt kg.",
                nn = "Setebredde: $setebredde cm, lårlengde: $lårlengde cm, legglengde: $legglengde cm, høgde: $høyde cm, kroppsvekt: $kroppsvekt kg.",
            )
        },
    )
}

fun seilEllerSele(hm: Hjelpemiddel): List<Opplysning> {
    /**
     * DIGIHOT-645
     * Badekarheis benyttes uten seil og dette spørsmålet
     * (Har bruker behov for seil eller sele til personløfteren? Ja/Nei)
     * er derfor ikke relevant for post 9. Bør derfor fjernes helt for post 9. Skal vises for de andre postene.
     */
    val apostnrBadekarheis = "9"
    if (hm.produkt?.kategori == "Personløftere og seil" &&
        hm.produkt.apostnr != apostnrBadekarheis &&
        hm.personløfterInfo?.harBehovForSeilEllerSele != null
    ) {
        return opplysninger(
            ledetekst = LokalisertTekst(
                nb = "Har bruker behov for seil eller sele",
                nn = "Har brukar behov for segl eller sele",
            ),
            tekst = when (hm.personløfterInfo.harBehovForSeilEllerSele) {
                true -> LokalisertTekst("Ja")
                false -> LokalisertTekst("Nei")
            },
        )
    }
    return emptyList()
}

fun appinfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.appInfo == null) {
        return emptyList()
    }
    val opplysninger = mutableListOf<Opplysning>()

    opplysninger.add(
        Opplysning(
            ledetekst = LokalisertTekst(nb = "Utprøving for bruker", nn = "Utprøving for brukar"),
            innhold = when (hm.appInfo.brukerHarPrøvdPrøvelisens) {
                true -> Tekst(
                    nb = "Bruker har hatt en vellykket utprøving av prøvelisensen.",
                    nn = "Brukar har hatt ei vellykka utprøving av prøvelisensen.",
                )

                false -> Tekst(
                    nb = "Bruker har ikke testet prøvelisensen. Informasjon er gitt om at bruker vil bli bedt om å teste prøvelisensen under behandling av saken.",
                    nn = "Brukar har ikkje testa prøvelisensen. Informasjon er gitt om at brukar vil bli bedd om å testa prøvelisensen under behandling av saka.",
                )
            },
        ),
    )

    opplysninger.add(
        Opplysning(
            ledetekst = LokalisertTekst("Støtteperson"),
            innhold = when (hm.appInfo.støttepersonSkalAdministrere) {
                true -> Tekst(
                    nb = "Støtteperson skal hjelpe bruker med kalenderen.",
                    nn = "Støtteperson skal hjelpa bruker med kalenderen.",
                )

                false -> Tekst(
                    nb = "Ingen støtteperson skal hjelpe bruker med kalenderen.",
                    nn = "Ingen støtteperson skal hjelpa brukar med kalenderen.",
                )
            },
        ),
    )

    if (hm.appInfo.støttepersonSkalAdministrere) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Utprøving for støtteperson"),
                innhold = when (hm.appInfo.støttepersonHarPrøvdPrøvelisens) {
                    true -> Tekst(
                        nb = "Støtteperson har hatt en vellykket utprøving av prøvelisensen.",
                        nn = "Støtteperson har hatt ei vellykka utprøving av prøvelisensen.",
                    )

                    false -> Tekst(
                        nb = "Støtteperson har ikke prøvd ut prøvelisensen. Informasjon er gitt om at støttepersonen vil bli bedt om å teste prøvelisensen under behandling av saken.",
                        nn = "Støtteperson har ikkje prøvd ut prøvelisensen. Informasjon er gitt om at støttepersonen vil bli bede om å testa prøvelisensen under behandling av saka.",
                    )

                    else -> error("hm.appInfo.støttepersonHarPrøvdPrøvelisens skal være satt når hm.appInfo.støttepersonSkalAdministrere==true")
                },
            ),
        )
    }

    return opplysninger
}

fun varmehjelpemiddelinfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.varmehjelpemiddelInfo == null) {
        return emptyList()
    }

    if (hm.varmehjelpemiddelInfo.harHelseopplysningerFraFør == true) {
        return opplysninger(
            ledetekst = LokalisertTekst(nb = "Opplysninger fra lege", nn = "Opplysningar frå lege"),
            tekst = Tekst(
                nb = "Det er automatisk sjekket at Nav har opplysninger fra lege fra før, eller at opplysningene oppbevares i kommunen",
                nn = "Det er automatisk sjekka at Nav har opplysningar frå lege frå før, eller at opplysningane blir oppbevarte i kommunen",
            ),
        )
    }

    val tekster = mutableListOf<Tekst>()
    if (hm.varmehjelpemiddelInfo.legeBekrefterDiagnose == true) {
        tekster.add(
            Tekst(
                nb = "Kommunen har opplysninger fra lege om diagnosen til bruker. Legen bekrefter at diagnosen gir nedsatt blodgjennomstrømning i hender eller føtter. Når bruker utsettes for kulde så får hender eller føtter unormal blekhet og cyanose.",
                nn = "Kommunen har opplysningar frå lege om diagnosen til brukar. Legen stadfestar at diagnosen gir nedsett blodgjennomstrømning i hender eller føter. Når brukar blir utsett for kulde så får hender eller føter unormal bleikheit og cyanose.",
            ),
        )
    }
    if (hm.varmehjelpemiddelInfo.opplysningerFraLegeOppbevaresIKommune == true) {
        tekster.add(
            Tekst(
                nb = "Opplysningene fra lege oppbevares i kommunen.",
                nn = "Opplysningane frå lege blir oppbevarte i kommunen.",
            ),
        )
    }

    return opplysninger(
        ledetekst = LokalisertTekst(nb = "Formidler bekrefter at", nn = "Formidlar stadfestar at"),
        tekster = tekster,
    )
}

fun sengeinfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.sengInfo == null) {
        return emptyList()
    }

    val opplysninger = mutableListOf<Opplysning>()
    val behovForSengLabel = LokalisertTekst("Behov for seng")

    // Seng har påkrevd behov
    if (hm.sengInfo.påkrevdBehov != null) {
        if (hm.sengInfo.påkrevdBehov == BehovForSeng.DYSFUNKSJONELT_SØVNMØNSTER) {
            val tekst =
                if (hm.sengInfo.brukerOppfyllerPåkrevdBehov == true) {
                    Tekst(
                        forhåndsdefinertTekst = LokalisertTekst(
                            nb = "Barnet har en varig funksjonsnedsettelse som gir et dysfunksjonelt søvnmønster slik at barnet får for lite søvn. Dette fører til at barnet får nedsatt funksjonsevne på dagtid.",
                            nn = "Barnet har ei varig funksjonsnedsetjing som gir eit dysfunksjonelt søvnmønster slik at barnet får for lite søvn. Dette fører til at barnet får nedsett funksjonsevne på dagtid.",
                        ),
                        begrepsforklaring = dysfunksjoneltSøvnmønsterForklaring,
                    )
                } else {
                    Tekst(
                        nb = "Barnet har <em>ikke</em> en varig funksjonsnedsettelse som gir et dysfunksjonelt søvnmønster slik at barnet får for lite søvn.",
                        nn = "Barnet har <em>ikkje</em> ei varig funksjonsnedsetjing som gir eit dysfunksjonelt søvnmønster slik at barnet får for lite søvn.",
                    )
                }
            opplysninger.add(Opplysning(behovForSengLabel, tekst))
        }

        if (hm.sengInfo.påkrevdBehov == BehovForSeng.STERKE_UFRIVILLIGE_BEVEGELSER) {
            val tekst =
                if (hm.sengInfo.brukerOppfyllerPåkrevdBehov == true) {
                    Tekst(
                        nb = "Bruker har behov for sengen på grunn av sterke ufrivillige bevegelser.",
                        nn = "Bruker har behov for senga på grunn av sterke ufrivillige rørsler.",
                    )
                } else {
                    Tekst(
                        nb = "Bruker har <em>ikke</em> behov for sengen på grunn av sterke ufrivillige bevegelser.",
                        nn = "Bruker har <em>ikkje</em> behov for senga på grunn av sterke ufrivillige rørsler.",
                    )
                }
            opplysninger.add(Opplysning(behovForSengLabel, tekst))
        }

        if (!hm.sengInfo.behovForSengBegrunnelse.isNullOrBlank()) {
            opplysninger.add(
                Opplysning(
                    ledetekst = LokalisertTekst("Grunnen til behovet"),
                    innhold = Tekst(hm.sengInfo.behovForSengBegrunnelse),
                ),
            )
        }
    }

    // Seng har ikke påkrevd behov, bare vis svaret som innsender gjorde
    if (hm.sengInfo.påkrevdBehov == null && hm.sengInfo.behovForSeng != null) {
        if (!hm.sengInfo.behovForSengBegrunnelse.isNullOrBlank()) {
            opplysninger.add(
                Opplysning(
                    ledetekst = LokalisertTekst("Grunnen til behovet"),
                    innhold = Tekst(hm.sengInfo.behovForSengBegrunnelse),
                ),
            )
        } else {
            if (hm.sengInfo.behovForSeng == BehovForSeng.DYSFUNKSJONELT_SØVNMØNSTER) {
                opplysninger.add(
                    Opplysning(
                        ledetekst = behovForSengLabel,
                        innhold = Tekst(
                            forhåndsdefinertTekst = LokalisertTekst(
                                nb = "Barnet har en varig funksjonsnedsettelse som gir et dysfunksjonelt søvnmønster slik at barnet får for lite søvn. Dette fører til at barnet får nedsatt funksjonsevne på dagtid.",
                                nn = "Barnet har ei varig funksjonsnedsetjing som gir eit dysfunksjonelt søvnmønster slik at barnet får for lite søvn. Dette fører til at barnet får nedset funksjonsevne på dagtid.",
                            ),
                            begrepsforklaring = dysfunksjoneltSøvnmønsterForklaring,
                        ),
                    ),
                )
            }
            if (hm.sengInfo.behovForSeng == BehovForSeng.RISIKO_FOR_FALL_UT_AV_SENG) {
                opplysninger.add(
                    Opplysning(
                        ledetekst = behovForSengLabel,
                        innhold = Tekst(
                            nb = "Barnet har varig nedsatt funksjonsevne som gir økt risiko for å falle ut av seng.",
                            nn = "Barnet har varig nedsett funksjonsevne som gir auka risiko for å falla ut av seng.",
                        ),
                    ),
                )
            }
        }
    }

    if (hm.sengInfo.høyGrindValg != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = bekreftetAvFormidler,
                innhold = Tekst(
                    nb = "Jeg er kjent med at denne sengen har et tvangsaspekt som er omtalt i Rundskriv til ftrl § 10-7. Jeg er også kjent med at det er jeg som har ansvaret for i hvilke sammenhenger hjelpemiddelet skal brukes, og for å følge opp hjelpemiddelbruken.",
                    nn = "Eg er kjend med at denne senga har eit tvangsaspekt som er omtalt i Rundskriv til ftrl § 10-7. Eg er også kjend med at det er eg som har ansvaret for i kva samanhengar hjelpemiddelet skal brukast, og for å følgja opp hjelpemiddelbruken.",
                ),
            ),
        )

        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Andre tiltak"),
                innhold = if (hm.sengInfo.høyGrindValg.harForsøktOpptrening) {
                    Tekst(
                        nb = "Det er forsøkt opptrening i ferdigheter eller andre tiltak som reduserer eller fjerner behovet for hjelpemiddelet.",
                        nn = "Det er prøvd opptrening i ferdigheiter eller andre tiltak som reduserer eller fjernar behovet for hjelpemiddelet.",
                    )
                } else {
                    Tekst(
                        nb = "Det er <em>ikke</em> forsøkt opptrening i ferdigheter eller tiltak som reduserer eller fjerner behovet for hjelpemiddelet.",
                        nn = "Det er <em>ikkje</em> prøvd opptrening i ferdigheiter eller tiltak som reduserer eller fjernar behovet for hjelpemiddelet.",
                    )
                },
            ),
        )

        if (!hm.sengInfo.høyGrindValg.harIkkeForsøktOpptreningBegrunnelse.isNullOrBlank()) {
            opplysninger.add(
                Opplysning(
                    ledetekst = LokalisertTekst(
                        nb = "Begrunnelse for at opptrening i ferdigheter eller andre tiltak ikke er forsøkt",
                        nn = "Grunngiving for at opptrening i ferdigheiter eller andre tiltak ikkje er prøvd",
                    ),
                    innhold = hm.sengInfo.høyGrindValg.harIkkeForsøktOpptreningBegrunnelse,
                ),
            )
        }

        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Plan"),
                innhold = if (hm.sengInfo.høyGrindValg.erLagetPlanForOppfølging) {
                    Tekst(
                        nb = "Det er laget en plan for oppfølging av bruken av hjelpemiddelet.",
                        nn = "Det er laga ein plan for oppfølging av bruken av hjelpemiddelet.",
                    )
                } else {
                    Tekst(
                        nb = "Det er <em>ikke</em> laget en plan for oppfølging av bruken av hjelpemiddelet.",
                        nn = "Det er <em>ikkje</em> laga ein plan for oppfølging av bruken av hjelpemiddelet.",
                    )
                },
            ),
        )
    }

    return opplysninger
}

fun elektriskVendesystemInfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.elektriskVendesystemInfo == null) {
        return emptyList()
    }

    val opplysninger = mutableListOf<Opplysning>()

    if (hm.elektriskVendesystemInfo.sengForMontering?.hmsnr != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst("Art.nr. på senga"),
                innhold = LokalisertTekst("${hm.elektriskVendesystemInfo.sengForMontering.hmsnr} ${hm.elektriskVendesystemInfo.sengForMontering.navn ?: ""}"),
            ),
        )
    }

    if (hm.elektriskVendesystemInfo.sengForMontering?.madrassbredde != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst(nb = "Madrassbredde", nn = "Madrassbreidd"),
                innhold = LokalisertTekst("${hm.elektriskVendesystemInfo.sengForMontering.madrassbredde} cm"),
            ),
        )
    }

    return opplysninger
}

fun posisjoneringssysteminfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.posisjoneringssystemInfo == null) {
        return emptyList()
    }

    val opplysninger = mutableListOf<Opplysning>()

    if (hm.posisjoneringssystemInfo.skalIkkeBrukesSomBehandlingshjelpemiddel == true ||
        hm.posisjoneringssystemInfo.skalIkkeBrukesTilRenSmertelindring == true
    ) {
        val tekster = mutableListOf<Tekst>()
        if (hm.posisjoneringssystemInfo.skalIkkeBrukesSomBehandlingshjelpemiddel == true) {
            tekster.add(
                Tekst(
                    nb = "Hjelpemidlene skal ikke brukes som et behandlingshjelpemiddel.",
                    nn = "Hjelpemidlene skal ikke brukes som et behandlingshjelpemiddel.",
                ),
            )
        }
        if (hm.posisjoneringssystemInfo.skalIkkeBrukesTilRenSmertelindring == true) {
            tekster.add(
                Tekst(
                    nb = "Hjelpemidlene skal ikke brukes til ren smertelindring. Dette gjelder selv om det bedrer funksjonsevnen.",
                    nn = "Hjelpemidla skal ikkje brukast til rein smertelindring. Dette gjeld sjølv om det betrar funksjonsevna.",
                ),
            )
        }
        opplysninger.add(Opplysning(ledetekst = formidlerBekrefterAt, innhold = tekster))
    }

    if (hm.posisjoneringssystemInfo.behov === PosisjoneringsputeBehov.STORE_LAMMELSER) {
        opplysninger.add(
            Opplysning(
                ledetekst = behov,
                innhold = Tekst(
                    nb = "Det er nødvendig for å kunne sitte eller ligge på grunn av store lammelser, feilstillinger og kontrakturer.",
                    nn = "Det er nødvendig for å kunna sitja eller liggja på grunn av store lammingar, feilstillingar og kontrakturar.",
                ),
            ),
        )
    }

    if (hm.posisjoneringssystemInfo.behov === PosisjoneringsputeBehov.DIREKTE_AVHJELPE_I_DAGLIGLIVET) {
        requireNotNull(hm.posisjoneringssystemInfo.oppgaverIDagliglivet) { "hm.posisjoneringssystemInfo.oppgaverIDagliglivet må være satt når hm.posisjoneringssystemInfo.behov === PosisjoneringsputeBehov.DIREKTE_AVHJELPE_I_DAGLIGLIVET" }
        val oppgaver = hm.posisjoneringssystemInfo.oppgaverIDagliglivet.map {
            when (it) {
                PosisjoneringsputeOppgaverIDagligliv.SPISE_DRIKKE_OL -> Tekst(
                    nb = "Spise, drikke, børste håret eller lignende aktiviteter i dagliglivet (ADL).",
                    nn = "Eta, drikka, børsta håret eller liknande aktivitetar i dagleglivet (ADL).",
                )

                PosisjoneringsputeOppgaverIDagligliv.BRUKE_DATAUTSTYR -> Tekst(
                    nb = "Bruke datautstyr for å gjøre nødvendige oppgaver, som for eksempel å betale regninger.",
                    nn = "Bruka datautstyr for å gjera nødvendige oppgåver, som til dømes å betala rekningar.",
                )

                PosisjoneringsputeOppgaverIDagligliv.FØLGE_OPP_BARN -> Tekst(
                    nb = "Følge opp barn.",
                    nn = "Følgja opp barn.",
                )

                PosisjoneringsputeOppgaverIDagligliv.HOBBY_FRITID_U26 -> Tekst(
                    nb = "Hobby og fritidsaktiviteter for personer under 26 år.",
                    nn = "Hobby og fritidsaktivitetar for personar under 26 år.",
                )

                PosisjoneringsputeOppgaverIDagligliv.ANNET -> Tekst(
                    hm.posisjoneringssystemInfo.oppgaverIDagliglivetAnnet
                        ?: error("hm.posisjoneringssystemInfo.oppgaverIDagliglivetAnnet må være satt når hm.posisjoneringssystemInfo.oppgaverIDagliglivet == ANNET"),
                )
            }
        }

        opplysninger.add(
            Opplysning(
                ledetekst = behov,
                innhold = LokalisertTekst(
                    nb = "Det vil direkte avhjelpe nedsatt funksjonsevne slik at dagliglivets oppgaver kan utføres.",
                    nn = "Det vil direkte avhjelpa nedsett funksjonsevne slik at oppgåvene til dagleglivet kan utførast.",
                ),
            ),
        )

        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst(
                    nb = "Oppgaver i dagliglivet som skal utføres",
                    nn = "Oppgåver i dagleglivet som skal utførast",
                ),
                innhold = oppgaver,
            ),
        )
    }

    return opplysninger
}

fun posisjoneringsputerForBarnInfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.posisjoneringsputeForBarnInfo == null) {
        return emptyList()
    }

    val opplysninger = mutableListOf<Opplysning>()

    if (hm.posisjoneringsputeForBarnInfo.bruksområde == PosisjoneringsputeForBarnBruk.TILRETTELEGGE_UTGANGSSTILLING) {
        opplysninger.add(
            Opplysning(
                ledetekst = bruksområde,
                innhold = Tekst(
                    nb = "Tilrettelegge utgangsstilling for aktivitet",
                    nn = "Leggja til rette utgangsstilling for aktivitet",
                ),
            ),
        )
    }

    if (hm.posisjoneringsputeForBarnInfo.bruksområde == PosisjoneringsputeForBarnBruk.TRENING_AKTIVITET_STIMULERING) {
        opplysninger.add(
            Opplysning(
                ledetekst = bruksområde,
                innhold = Tekst(
                    nb = "Til trening, aktivitet og stimulering",
                    nn = "Til trening, aktivitet og stimulering",
                ),
            ),
        )
        if (hm.posisjoneringsputeForBarnInfo.detErLagetEnMålrettetPlan == true ||
            hm.posisjoneringsputeForBarnInfo.planenOppbevaresIKommunen == true
        ) {
            val tekster = mutableListOf<Tekst>()

            if (hm.posisjoneringsputeForBarnInfo.detErLagetEnMålrettetPlan == true) {
                tekster.add(
                    Tekst(
                        nb = "Det er laget en målrettet plan der funksjonsnivå, målene og tiltak for å nå målene er beskrevet.",
                        nn = "Det er laga ein målretta plan der funksjonsnivå, måla og tiltak for å nå måla er beskrivne.",
                    ),
                )
            }

            if (hm.posisjoneringsputeForBarnInfo.planenOppbevaresIKommunen == true) {
                tekster.add(
                    Tekst(
                        nb = "Planen oppbevares i kommunen.",
                        nn = "Planen blir oppbevart i kommunen.",
                    ),
                )
            }

            opplysninger.add(
                Opplysning(
                    ledetekst = formidlerBekrefterAt,
                    innhold = tekster,
                ),
            )
        }
    }

    return opplysninger
}

fun diverseinfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.diverseInfo == null) {
        return emptyList()
    }

    val opplysninger = mutableListOf<Opplysning>()

    if (hm.diverseInfo.takhøydeStøttestangCm != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst(nb = "Takhøyde", nn = "Takhøgde"),
                innhold = LokalisertTekst("${hm.diverseInfo.takhøydeStøttestangCm} cm"),
            ),
        )
    }

    if (hm.diverseInfo.sitteputeSkalBrukesIRullestolFraNav == true) {
        opplysninger.add(
            Opplysning(
                ledetekst = formidlerBekrefterAt,
                innhold = Tekst(
                    nb = "Sitteputen skal kun brukes i en rullestol som er utlånt fra Nav.",
                    nn = "Sitjeputa skal berre brukast i ein rullestol som er utlånt frå Nav.",
                ),
            ),
        )
    }

    return opplysninger
}

fun oppreisningsstolInfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.oppreisningsstolInfo == null) {
        return emptyList()
    }

    val opplysninger = mutableListOf<Opplysning>()

    opplysninger.add(
        Opplysning(
            ledetekst = LokalisertTekst("Funksjon"),
            innhold = if (hm.oppreisningsstolInfo.kanBrukerReiseSegSelvFraVanligStol) {
                Tekst(
                    nb = "Personen kan reise seg selv fra vanlige stoler ved bruk av enklere tiltak som for eksempel forhøyningsklosser, puter, støttestang, støttehåndtak og lignende.",
                    nn = "Personen kan reisa seg sjølv frå vanlege stolar ved bruk av enklare tiltak som til dømes løfteklossar, puter, støttestong, støttehandtak og liknande.",
                )
            } else {
                Tekst(
                    nb = "Personen kan ikke reise seg selv fra vanlige stoler ved bruk av enklere tiltak som for eksempel forhøyningsklosser, puter, støttestang, støttehåndtak og lignende.",
                    nn = "Personen kan ikkje reisa seg sjølv frå vanlege stolar ved bruk av enklare tiltak som til dømes løfteklossar, puter, støttestong, støttehandtak og liknande.",
                )
            },
        ),
    )

    if (hm.oppreisningsstolInfo.bruksområde != null) {
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst(nb = "Stolen skal brukes i", nn = "Stolen skal brukast i"),
                innhold = when (hm.oppreisningsstolInfo.bruksområde) {
                    OppreisningsstolBruksområde.EGEN_BOENHET -> Tekst(
                        nb = "Personens egen boenhet.",
                        nn = "Personen si eiga bueining.",
                    )

                    OppreisningsstolBruksområde.FELLESAREAL -> Tekst(
                        nb = "Fellesarealer og være fast plassert der.",
                        nn = "Fellesareal og vera fast plassert der.",
                    )
                },
            ),
        )
    }

    if (!hm.oppreisningsstolInfo.behovForStolBegrunnelse.isNullOrBlank()) {
        opplysninger.add(
            Opplysning(
                ledetekst = grunnenTilBehovet,
                innhold = hm.oppreisningsstolInfo.behovForStolBegrunnelse,
            ),
        )
    } else {
        opplysninger.add(
            Opplysning(
                ledetekst = grunnenTilBehovet,
                innhold = hm.oppreisningsstolInfo.behov?.map { behov ->
                    when (behov) {
                        OppreisningsstolBehov.OPPGAVER_I_DAGLIGLIVET -> Tekst(
                            nb = "Personen skal reise seg opp og utføre dagliglivets oppgaver.",
                            nn = "Personen skal reisa seg opp og utføra oppgåvene til dagleglivet.",
                        )

                        OppreisningsstolBehov.PLEID_I_HJEMMET -> Tekst(
                            nb = "Oppreisningsfunksjonen er nødvendig for at personen skal bli pleid i hjemmet.",
                            nn = "Oppreisingsfunksjonen er nødvendig for at personen skal bli pleidd i heimen.",
                        )

                        OppreisningsstolBehov.FLYTTE_MELLOM_STOL_OG_RULLESTOL -> Tekst(
                            nb = "Personen har ikke ståfunksjon og skal flytte seg selv mellom rullestol og stol med oppreisningsfunksjon.",
                            nn = "Personen har ikkje ståfunksjon og skal flytta seg sjølv mellom rullestol og stol med oppreisingsfunksjon.",
                        )
                    }
                }
                    ?: error("hm.oppreisningsstolInfo.behov må være satt når hm.oppreisningsstolInfo.behovForStolBegrunnelse ikke er satt."),
            ),
        )
    }

    opplysninger.add(
        Opplysning(
            ledetekst = LokalisertTekst("Trekk"),
            innhold = when (hm.oppreisningsstolInfo.annetTrekkKanBenyttes) {
                true -> Tekst(nb = "Stol i annet trekk kan benyttes", nn = "Stol i anna trekk kan nyttast")
                false -> Tekst(
                    nb = "Stol i annet trekk kan ikke benyttes",
                    nn = "Stol i anna trekk kan ikkje nyttast",
                )
            },
        ),
    )

    opplysninger.add(
        Opplysning(
            ledetekst = LokalisertTekst("Skråløft eller rettløft"),
            innhold = when (hm.oppreisningsstolInfo.løftType) {
                OppreisningsstolLøftType.SKRÅLØFT -> LokalisertTekst("Skråløft")
                OppreisningsstolLøftType.RETTLØFT -> LokalisertTekst("Rettløft")
            },
        ),
    )

    return opplysninger
}

fun ganghjelpemiddelInfo(hm: Hjelpemiddel): List<Opplysning> {
    if (hm.ganghjelpemiddelInfo == null) {
        return emptyList()
    }

    val opplysninger = mutableListOf<Opplysning>()

    if (hm.ganghjelpemiddelInfo.kanIkkeBrukeMindreAvansertGanghjelpemiddel == true &&
        hm.ganghjelpemiddelInfo.type != null
    ) {
        opplysninger.add(
            Opplysning(
                ledetekst = formidlerBekrefterAt,
                innhold = when (hm.ganghjelpemiddelInfo.type) {
                    GanghjelpemiddelType.GÅBORD -> Tekst(
                        nb = "Innbygger ikke kan bruke gåbord med manuell høyderegulering. Dette er på grunn av funksjonsnedsettelsen personen har.",
                        nn = "Innbyggjar ikkje kan bruka gåbord med manuell høgderegulering. Dette er på grunn av funksjonsnedsetjinga personen har.",
                    )

                    GanghjelpemiddelType.SPARKESYKKEL -> Tekst(
                        nb = "Innbygger ikke kan bruke vanlig sparkesykkel. Med vanlig sparkesykkel menes sparkesykler som ikke er utviklet spesielt for personer med funksjonsnedsettelse.",
                        nn = "Innbyggjar ikkje kan bruka vanleg sparkesykkel. Med vanleg sparkesykkel siktar ein til sparkesyklar som ikkje er utvikla spesielt for personar med funksjonsnedsetjing.",
                    )

                    GanghjelpemiddelType.KRYKKE -> Tekst(
                        nb = "Innbygger ikke kan bruke krykke uten støtdemping på grunn av sin funksjonsnedsettelse.",
                        nn = "Innbyggjar ikkje kan bruka krykkje utan støytdemping på grunn av funksjonsnedsetjinga si.",
                    )

                    else -> error("Uventet verdi for hm.ganghjelpemiddelInfo.type: ${hm.ganghjelpemiddelInfo.type}")
                },
            ),
        )
    }

    if (hm.ganghjelpemiddelInfo.bruksområde != null) {
        val type = hm.ganghjelpemiddelInfo.type
            ?: error("hm.ganghjelpemiddelInfo.type skal være satt når hm.ganghjelpemiddelInfo.bruksområde er satt")
        val bruksområde = hm.ganghjelpemiddelInfo.bruksområde
        val tekst = when (type) {
            GanghjelpemiddelType.GÅSTOL -> when (bruksområde) {
                BruksområdeGanghjelpemiddel.TIL_FORFLYTNING -> LokalisertTekst(
                    nb = "Til forflytning",
                    nn = "Til forflytting",
                )

                BruksområdeGanghjelpemiddel.TIL_TRENING_OG_ANNET -> LokalisertTekst("Til trening, aktivisering og stimulering")
            }

            GanghjelpemiddelType.SPARKESYKKEL -> when (bruksområde) {
                BruksområdeGanghjelpemiddel.TIL_FORFLYTNING -> when (hm.ganghjelpemiddelInfo.brukerErFylt26År) {
                    true -> LokalisertTekst(
                        nb = "Til forflytning. Den skal ikke brukes til trening, aktivisering og stimulering.",
                        nn = "Til forflytting. Den skal ikkje brukast til trening, aktivisering og stimulering.",
                    )

                    else -> LokalisertTekst(
                        nb = "Til forflytning ved nedsatt gangfunksjon",
                        nn = "Til forflytting ved nedsett gangfunksjon",
                    )
                }

                BruksområdeGanghjelpemiddel.TIL_TRENING_OG_ANNET -> LokalisertTekst(
                    nb = "Til leke- og sportsaktiviteter, trening og stimulering",
                    nn = "Til leike- og sportsaktivitetar, trening og stimulering",
                )
            }

            GanghjelpemiddelType.GÅTRENING -> when (bruksområde) {
                BruksområdeGanghjelpemiddel.TIL_FORFLYTNING -> LokalisertTekst("Til trening av gangfunksjonen")
                else -> error("Forventet ikke type=$type og bruksområde=$bruksområde for hm.ganghjelpemiddelInfo")
            }

            else -> error("Forventet ikke type=$type og bruksområde=$bruksområde for hm.ganghjelpemiddelInfo")
        }
        opplysninger.add(
            Opplysning(
                ledetekst = LokalisertTekst(nb = "Hovedformål", nn = "Hovudformål"),
                innhold = tekst,
            ),
        )
    }

    if (hm.ganghjelpemiddelInfo.detErLagetEnMålrettetPlan == true || hm.ganghjelpemiddelInfo.planenOppbevaresIKommunen == true) {
        val tekster = mutableListOf<Tekst>()
        if (hm.ganghjelpemiddelInfo.detErLagetEnMålrettetPlan == true) {
            tekster.add(
                Tekst(
                    nb = "Det er laget en målrettet plan der funksjonsnivå, målene og tiltak for å nå målene er beskrevet.",
                    nn = "Det er laga ein målretta plan der funksjonsnivå, måla og tiltak for å nå måla er beskrivne.",
                ),
            )
        }
        if (hm.ganghjelpemiddelInfo.planenOppbevaresIKommunen == true) {
            tekster.add(Tekst(nb = "Planen oppbevares i kommunen.", nn = "Planen blir oppbevart i kommunen."))
        }
        opplysninger.add(
            Opplysning(
                ledetekst = formidlerBekrefterAt,
                innhold = tekster,
            ),
        )
    }

    return opplysninger
}

private fun opplysninger(ledetekst: LokalisertTekst, tekster: List<Tekst>) = listOf(Opplysning(ledetekst, tekster))

private fun opplysninger(ledetekst: LokalisertTekst, tekst: Tekst) = listOf(Opplysning(ledetekst, tekst))

private fun opplysninger(ledetekst: LokalisertTekst, tekst: LokalisertTekst) = listOf(Opplysning(ledetekst, Tekst(tekst)))

private fun opplysninger(ledetekst: LokalisertTekst, tekst: String) = listOf(Opplysning(ledetekst, Tekst(tekst)))

private val bekreftetAvFormidler = LokalisertTekst(nb = "Bekreftet av formidler", nn = "Stadfesta av formidlar")
private val formidlerBekrefterAt = LokalisertTekst(nb = "Formidler bekrefter at", nn = "Formidlar stadfestar at")
private val behov = LokalisertTekst("Behov")
private val bruksområde = LokalisertTekst("Bruksområde")
private val grunnenTilBehovet = LokalisertTekst("Grunnen til behovet")
private val dysfunksjoneltSøvnmønsterForklaring = LokalisertTekst(
    nb = "Med dysfunksjonelt søvnmønster menes: Varige og vesentlige problemer med å sovne, urolig nattesøvn, meget tidlig oppvåkning om morgenen og/eller dårlig søvnkvalitet som fører til nedsatt funksjon på dagtid. Den nedsatte funksjonen på dagtid må føre til problemer med å utføre dagliglivets nødvendige aktiviteter.",
    nn = "Med dysfunksjonelt søvnmønster siktar ein til: Varige og vesentlege problem med å sovna, uroleg nattesøvn, svært tidleg oppvakning om morgonen og/eller dårleg søvnkvalitet som fører til nedsett funksjon på dagtid. Den nedsette funksjonen på dagtid må føra til problem med å utføra dei nødvendige aktivitetane til dagleglivet.",
)

private fun padIso8(isocode: String?): Iso8? {
    if (isocode == null) return null

    if (isocode.length == 7) {
        return Iso8(isocode.padStart(8, '0'))
    }

    return Iso8(isocode)
}

private fun parseRangering(rangering: String?): Int? {
    if (rangering.isNullOrBlank()) {
        return null
    }
    return rangering.toInt()
}
