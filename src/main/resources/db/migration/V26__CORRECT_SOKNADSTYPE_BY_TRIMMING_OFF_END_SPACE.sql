UPDATE v1_infotrygd_data SET soknadstype = TRIM(TRAILING FROM soknadstype) WHERE soknadstype IS NOT NULL;
