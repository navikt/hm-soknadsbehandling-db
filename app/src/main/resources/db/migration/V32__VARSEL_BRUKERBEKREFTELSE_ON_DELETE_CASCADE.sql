ALTER TABLE V1_VARSEL_BRUKERBEKREFTELSE_BEHOVSMELDING
    DROP CONSTRAINT v1_varsel_brukerbekreftelse_behovsmelding_varsel_id_fkey,
    ADD CONSTRAINT v1_varsel_brukerbekreftelse_behovsmelding_varsel_id_fkey
        FOREIGN KEY (VARSEL_ID)
            REFERENCES V1_VARSEL_BRUKERBEKREFTELSE (ID)
            ON DELETE CASCADE;
