CREATE TABLE IF NOT EXISTS V1_HOTSAK_DATA
(
    SOKNADS_ID       UUID                     NOT NULL,
    SAKSNUMMER       VARCHAR(10)              NOT NULL,
    VEDTAKSRESULTAT  VARCHAR(10)              NULL,
    VEDTAKSDATO      DATE                     NULL,
    CREATED          TIMESTAMP                NOT NULL default (now()),
    PRIMARY KEY (SOKNADS_ID)
);
