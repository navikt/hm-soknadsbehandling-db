FROM ghcr.io/navikt/baseimages/temurin:17

COPY build/libs/hm-soknadsbehandling-db.jar app.jar
