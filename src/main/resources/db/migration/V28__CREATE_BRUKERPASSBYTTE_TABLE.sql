CREATE TABLE IF NOT EXISTS v1_brukerpassbytte
(
    id            UUID      NOT NULL,
    fnr_bruker    CHAR(11)  NOT NULL,
    data          JSONB     NOT NULL,
    created       TIMESTAMP NOT NULL default (now()),
    updated       TIMESTAMP NOT NULL default (now()),
    bytte_gjelder TEXT      NOT NULL,
    oppgaveid     BIGINT,
    journalpostid BIGINT,
    PRIMARY KEY (fnr_bruker, id)
);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
