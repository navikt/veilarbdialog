DO
$$
    BEGIN
        IF (SELECT exists(SELECT rolname FROM pg_roles WHERE rolname = 'cloudsqliamuser'))
        THEN
            GRANT USAGE ON SCHEMA veilarbdialog to "cloudsqliamuser";
            GRANT SELECT ON ALL TABLES IN SCHEMA veilarbdialog TO "cloudsqliamuser";
            ALTER DEFAULT PRIVILEGES IN SCHEMA veilarbdialog GRANT SELECT ON TABLES TO "cloudsqliamuser";
        END IF;
    END
$$;