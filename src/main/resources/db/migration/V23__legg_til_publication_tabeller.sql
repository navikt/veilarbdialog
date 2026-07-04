DO $$
    DECLARE
        onskede_tabeller text[] := ARRAY[
            'dialog',
            'dialog_aktor',
            'dialog_egenskap',
            'dialog_egenskap_type',
            'ekstern_varsel_kvittering',
            'eskaleringsvarsel',
            'event',
            'event_type',
            'feilede_kafka_aktor_id',
            'henvendelse',
            'id_mappinger',
            'kladd',
            'min_side_varsel',
            'min_side_varsel_dialog_mapping',
            'oversikten_melding_med_metadata',
            'paragraf8varsel',
            'schema_version',
            'shedlock',
            'siste_oppfolgingsperiode',
            'varsel'
        ];
        tabell_liste text;
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication WHERE pubname = 'ds_publication'
        ) THEN
            CREATE PUBLICATION ds_publication;
        END IF;

        -- En publication som er definert FOR ALL TABLES dekker allerede alt,
        -- og kan ikke endres med SET TABLE.
        IF EXISTS (
            SELECT 1 FROM pg_publication WHERE pubname = 'ds_publication' AND puballtables
        ) THEN
            RETURN;
        END IF;

        -- Ta bare med tabeller som faktisk finnes i databasen.
        SELECT string_agg(format('veilarbdialog.%I', t), ', ')
        INTO tabell_liste
        FROM unnest(onskede_tabeller) AS t
        WHERE to_regclass(format('veilarbdialog.%I', t)) IS NOT NULL;

        IF tabell_liste IS NOT NULL THEN
            EXECUTE 'ALTER PUBLICATION ds_publication SET TABLE ' || tabell_liste;
        END IF;
    END;
$$;
