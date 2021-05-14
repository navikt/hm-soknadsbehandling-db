# README
![build-deploy-dev](https://github.com/navikt/hm-soknadsbehandling-db/workflows/Build%20and%20deploy/badge.svg)

Api foran database som brukes av søknadsbehandling og brukers/formidlers søknadsvisning 


# Lokal køyring

1. Kjør docker-compose i [hm-soknad-api](https://github.com/navikt/hm-soknad-api) for å starte nødvendig økosystem:
```
cd hm-soknad-api
docker-compose -f docker-compose/docker-compose.yml up
```
- start hm-soknadsbehandling-db gjennom Application run configuration i Idea

Hvis du vil se søknadsdata i front-end se readmes for disse: [hm-formidler](https://github.com/navikt/hm-formidler) 
og [hm-dinehjelpemidler](https://github.com/navikt/hm-dinehjelpemidler) 


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #digihot-dev.
