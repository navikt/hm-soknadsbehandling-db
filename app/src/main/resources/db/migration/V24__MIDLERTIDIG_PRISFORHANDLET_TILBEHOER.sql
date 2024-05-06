-- Midlertidig tabell for å samle statistikk om bruk av tilbehør som er prisforhandlet vs. ikke. Denne blir dropped når
-- den ikke er relevant lengre.
CREATE TABLE IF NOT EXISTS v1_midlertidig_prisforhandlet_tilbehoer (
    id                  SERIAL      NOT NULL,
    hmsnr_hjelpemiddel  TEXT        NOT NULL,
    hmsnr_tilbehoer     TEXT        NOT NULL,
    rammeavtale_id      TEXT        NOT NULL,
    leverandor_id       TEXT        NOT NULL,
    prisforhandlet      BOOLEAN     NOT NULL,
    soknads_id          UUID        NOT NULL,
    created             TIMESTAMP   NOT NULL DEFAULT (now()),
    PRIMARY KEY (id)
);

CREATE INDEX v1_midlertidig_prisforhandlet_tilbehoer_prisforhandlet_idx ON v1_midlertidig_prisforhandlet_tilbehoer (prisforhandlet);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
