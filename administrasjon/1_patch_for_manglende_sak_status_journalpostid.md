# Patch for manglende sak, status og journalpostId for 1dd81910-52eb-47c6-967f-410c2fa0b686

Det ble varslet om søknad uten oppgave. Det viste seg at det ikke hadde blitt opprettet sak i databasen med saksnummer
fra Hotsak. Det manglet også statusoppdatering og journalpostId etter journalføring av søknaden. Patchen ble kjørt mot
produksjon 28.11.2023 09:08.

```PostgreSQL
-- Sett status til ENDELIG_JOURNALFØRT
INSERT INTO v1_status (id, soknads_id, status, created, begrunnelse, arsaker)
VALUES (DEFAULT, '1dd81910-52eb-47c6-967f-410c2fa0b686', 'ENDELIG_JOURNALFØRT', DEFAULT, NULL, NULL);

-- Opprett sak
INSERT INTO v1_hotsak_data (soknads_id, saksnummer, vedtaksresultat, vedtaksdato, created)
VALUES ('1dd81910-52eb-47c6-967f-410c2fa0b686', '170805', NULL, NULL, DEFAULT);

-- Set journalpostId
UPDATE v1_soknad
SET journalpostid = '634470523'
WHERE soknads_id = '1dd81910-52eb-47c6-967f-410c2fa0b686';
```
