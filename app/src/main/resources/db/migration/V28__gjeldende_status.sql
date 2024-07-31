CREATE OR REPLACE VIEW v1_gjeldende_status AS
SELECT DISTINCT ON (soknads_id) id, soknads_id, status, created, begrunnelse, arsaker
FROM v1_status
ORDER BY soknads_id, created DESC;
