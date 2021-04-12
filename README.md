# README
![build-deploy-dev](https://github.com/navikt/hm-soknadsbehandling-db/workflows/Build%20and%20deploy/badge.svg)

Api foran database som brukes av søknadsbehandling og brukers/formidlers søknadsvisning 


# Lokal køyring

Kjør docker-compose for å starte database lokalt: 
```
docker-compose -f docker-compose/docker-compose.yml up
```

- start [backend](https://github.com/navikt/hm-soknad-api) for å starte rapid og evt. populere rapid
- start [hm-soknadsbehandling-db](https://github.com/navikt/hm-soknadsbehandling-db) for å lagre søknad i db og sende videre på rapid

- start hm-soknadsbehandling-db og vent på melding


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #digihot-dev.
