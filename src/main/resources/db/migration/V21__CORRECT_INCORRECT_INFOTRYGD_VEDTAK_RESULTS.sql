-- The following update-mutations will fix data erroneously stored in the database with spaces due to fixed-size columns
-- in the Infotrygd database.

UPDATE V1_STATUS SET status='VEDTAKSRESULTAT_INNVILGET'
    WHERE soknads_id IN (SELECT SOKNADS_ID FROM V1_INFOTRYGD_DATA WHERE vedtaksresultat = 'I ' OR vedtaksresultat = ' I')
    AND status = 'VEDTAKSRESULTAT_ANNET';

UPDATE V1_STATUS SET status='VEDTAKSRESULTAT_AVSLÅTT'
    WHERE soknads_id IN (SELECT SOKNADS_ID FROM V1_INFOTRYGD_DATA WHERE vedtaksresultat = 'A ' OR vedtaksresultat = ' A')
    AND status = 'VEDTAKSRESULTAT_ANNET';

UPDATE V1_INFOTRYGD_DATA SET vedtaksresultat='I' WHERE vedtaksresultat = 'I ' OR vedtaksresultat = ' I';
UPDATE V1_INFOTRYGD_DATA SET vedtaksresultat='A' WHERE vedtaksresultat = 'A ' OR vedtaksresultat = ' A';
