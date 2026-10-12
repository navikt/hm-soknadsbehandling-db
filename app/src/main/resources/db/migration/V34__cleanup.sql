-- Sletter ubrukt tabell.
DROP TABLE IF EXISTS v1_brukerpassbytte;

-- Endrer til standardnavn.
ALTER INDEX v1_oebs_data_fnr_bruker_ordre_idx RENAME TO v1_oebs_data_fnr_bruker_idx;
ALTER INDEX idx_soknad_created RENAME TO v1_soknad_created_idx;

-- Endrer kolonnenavn til snake_case.
ALTER TABLE v1_hotsak_data
    RENAME saksnummer TO sak_id;
ALTER TABLE v1_soknad
    RENAME journalpostid TO journalpost_id;
ALTER TABLE v1_soknad
    RENAME oppgaveid TO oppgave_id;

-- Endre type til TEXT.
ALTER TABLE v1_hotsak_data
    ALTER COLUMN sak_id TYPE TEXT USING sak_id::TEXT;
ALTER TABLE v1_infotrygd_data
    ALTER COLUMN vedtaksresultat TYPE TEXT USING vedtaksresultat::TEXT;
ALTER TABLE v1_soknad
    ALTER COLUMN journalpost_id TYPE TEXT USING journalpost_id::TEXT,
    ALTER COLUMN oppgave_id TYPE TEXT USING oppgave_id::TEXT,
    ALTER COLUMN navn_bruker TYPE TEXT USING navn_bruker::TEXT;

-- Legger til manglende indekser.
CREATE INDEX ON v1_midlertidig_prisforhandlet_tilbehoer (soknads_id);
CREATE INDEX ON v1_varsel_brukerbekreftelse_behovsmelding (varsel_id);
CREATE INDEX ON v1_varsel_brukerbekreftelse_behovsmelding (behovsmelding_id);
