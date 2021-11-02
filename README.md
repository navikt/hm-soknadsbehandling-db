# README

![build-deploy-dev](https://github.com/navikt/hm-soknadsbehandling-db/workflows/Build%20and%20deploy/badge.svg)

Api foran database som brukes av søknadsbehandling og brukers/formidlers søknadsvisning

# Oppdatering av klientkode for Hjelpemiddeldatabasen

Kjør hm-grunndata-api lokalt (endre evt. også URL i build.gradle.kts). Kjør så følgende kommando for å generere
`src/main/resources/hmdb/schema.graphql`.

```bash
$ ./gradlew graphqlIntrospectSchema
```

Lag nye eller endre eksisterende spørringer i `src/main/resources/hmdb`. Under bygg vil det genereres klientkode basert
på skjemaet og spørringene som er definert.

# Lokal køyring

1. Kjør docker-compose i [hm-soknad-api](https://github.com/navikt/hm-soknad-api) for å starte nødvendig økosystem:

```bash
$ cd hm-soknad-api
$ docker-compose -f docker-compose/docker-compose.yml up
```

- start hm-soknadsbehandling-db gjennom Application run configuration i Idea

Hvis du vil se søknadsdata i front-end se readmes for disse: [hm-formidler](https://github.com/navikt/hm-formidler)
og [hm-dinehjelpemidler](https://github.com/navikt/hm-dinehjelpemidler)

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #digihot-dev.
