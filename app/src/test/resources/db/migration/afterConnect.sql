DO
$$
    BEGIN
        CREATE ROLE cloudsqliamuser;
    EXCEPTION
        WHEN duplicate_object THEN RAISE NOTICE '%, skipping', sqlerrm USING ERRCODE = sqlstate;
    END
$$;
