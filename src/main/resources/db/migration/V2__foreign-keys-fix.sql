-- Nødvendig å kjøre siden GCP's migreringsverktøy ikke klarte å lage disse fremmednøklene i Postgres
-- pga. at kolonnetypene var noe forskjellige
alter table dialog_egenskap
    alter column dialog_id type bigint using dialog_id::bigint;

alter table dialog_egenskap
    add constraint dialog_egenskap_dialog_id_fk
        foreign key (dialog_id) references dialog(dialog_id);

alter table eskaleringsvarsel
    alter column TILHORENDE_BRUKERNOTIFIKASJON_ID type bigint using TILHORENDE_BRUKERNOTIFIKASJON_ID::bigint;

alter table eskaleringsvarsel
    add constraint TILHORENDE_BRUKERNOTIFIKASJON_ID_FK
        foreign key (TILHORENDE_BRUKERNOTIFIKASJON_ID) references brukernotifikasjon(id);

alter table eskaleringsvarsel
    alter column tilhorende_dialog_id type bigint using tilhorende_dialog_id::bigint;

alter table eskaleringsvarsel
    add constraint TILHORENDE_DIALOG_ID_FK
        foreign key (TILHORENDE_DIALOG_ID) references dialog(dialog_id);
