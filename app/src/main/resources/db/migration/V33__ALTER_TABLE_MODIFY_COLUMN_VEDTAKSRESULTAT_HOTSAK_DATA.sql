-- Vedtaksresultater i hotsak kommer i tekstform, ikke kode
ALTER TABLE V1_HOTSAK_DATA
    ALTER COLUMN VEDTAKSRESULTAT TYPE TEXT;
