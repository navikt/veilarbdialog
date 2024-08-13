create schema veilarbdialog;
create sequence dialog_egenskap_rowid_seq
    increment by -1;

alter sequence dialog_egenskap_rowid_seq owner to veilarbdialog;

create sequence ekstern_varsel_kvittering_rowid_seq
    increment by -1;

alter sequence ekstern_varsel_kvittering_rowid_seq owner to veilarbdialog;

create sequence feilede_kafka_aktor_id_rowid_seq
    increment by -1;

alter sequence feilede_kafka_aktor_id_rowid_seq owner to veilarbdialog;

create sequence kladd_rowid_seq
    increment by -1;

alter sequence kladd_rowid_seq owner to veilarbdialog;

create sequence paragraf8varsel_rowid_seq
    increment by -1;

alter sequence paragraf8varsel_rowid_seq owner to veilarbdialog;

create sequence dialog_id_seq
    start with 618887
    cache 20;

alter sequence dialog_id_seq owner to veilarbdialog;

create sequence event_id_seq
    start with 875927
    cache 20;

alter sequence event_id_seq owner to veilarbdialog;

create sequence henvendelse_id_seq
    start with 1148378
    cache 20;

alter sequence henvendelse_id_seq owner to veilarbdialog;

create sequence iseq$$_130623
    start with 2998
    cache 20;

alter sequence iseq$$_130623 owner to veilarbdialog;

create sequence iseq$$_130627
    cache 20;

alter sequence iseq$$_130627 owner to veilarbdialog;

create sequence iseq$$_130634
    start with 311040
    cache 20;

alter sequence iseq$$_130634 owner to veilarbdialog;

create sequence kladd_id_seq
    start with 10288
    cache 20;

alter sequence kladd_id_seq owner to veilarbdialog;

create table shedlock
(
    name       varchar(64) not null
        primary key,
    lock_until timestamp(3),
    locked_at  timestamp(3),
    locked_by  varchar(255)
);

alter table shedlock
    owner to veilarbdialog;

grant delete, insert, select, update on shedlock to veilarbdialog_midlertidig;

create table dialog
(
    dialog_id                  bigint       not null
        constraint dialog_pk
            primary key,
    aktor_id                   varchar(255),
    overskrift                 varchar(255),
    lest_av_bruker_tid         timestamp(6),
    lest_av_veileder_tid       timestamp(6),
    aktivitet_id               varchar(64)
        constraint unique_aktivitet_id
            unique,
    siste_status_endring       timestamp(6),
    siste_vente_pa_svar_tid    timestamp(6),
    siste_ferdigbehandlet_tid  timestamp(6),
    historisk                  smallint,
    opprettet_dato             timestamp(6),
    siste_ubehandlet_tid       timestamp(6),
    eldste_uleste_for_bruker   timestamp(6),
    eldste_uleste_for_veileder timestamp(6),
    venter_pa_nav_siden        timestamp(6),
    venter_pa_svar_fra_bruker  timestamp(6),
    oppdatert                  timestamp(6) not null,
    kontorsperre_enhet_id      varchar(255),
    ulestparagraf8varsel       smallint,
    paragraf8_varsel_uuid      varchar(255),
    oppfolgingsperiode_uuid    varchar(40),
    arena_id                   varchar(64)
        constraint dialog_arenaid_unique
            unique
);

alter table dialog
    owner to veilarbdialog;

create index dialog_aktor_idx
    on dialog (aktor_id);

create index dialog_oppfolgingsperiode_idx
    on dialog ((1));

create table dialog_egenskap_type
(
    dialog_type    varchar(255) not null
        constraint dialog_egenskap_type_pk
            primary key,
    opprettet_dato timestamp(6) not null,
    opprettet_av   varchar(255) not null,
    endret_dato    timestamp(6) not null,
    endret_av      varchar(255) not null
);

alter table dialog_egenskap_type
    owner to veilarbdialog;

create table dialog_egenskap
(
    dialog_egenskap_type_kode varchar(255)                                                                     not null
        constraint dialog_egenskap_type_fk
            references dialog_egenskap_type,
    dialog_id                 numeric                                                                          not null,
    rowid                     numeric(33) default nextval('dialog_egenskap_rowid_seq'::regclass) not null
        constraint veilarbdialog_dialog_egenskap_pk_rowid
            primary key
);

alter table dialog_egenskap
    owner to veilarbdialog;

alter sequence dialog_egenskap_rowid_seq owned by dialog_egenskap.rowid;

create index dialog_id_egenskap_idx
    on dialog_egenskap (dialog_id);

create table ekstern_varsel_kvittering
(
    tidspunkt                        timestamp(6)                                                                               not null,
    brukernotifikasjon_bestilling_id varchar(255)                                                                               not null,
    doknotifikasjon_status           varchar(255)                                                                               not null,
    melding                          varchar(1024)                                                                              not null,
    distribusjon_id                  numeric,
    json_payload                     text                                                                                       not null,
    rowid                            numeric(33) default nextval('ekstern_varsel_kvittering_rowid_seq'::regclass) not null
        constraint veilarbdialog_ekstern_varsel_kvittering_pk_rowid
            primary key
);

alter table ekstern_varsel_kvittering
    owner to veilarbdialog;

alter sequence ekstern_varsel_kvittering_rowid_seq owned by ekstern_varsel_kvittering.rowid;

create table event_type
(
    event varchar(255) not null
        constraint event_type_pk
            primary key
);

alter table event_type
    owner to veilarbdialog;

create table event
(
    event_id     bigint       not null
        constraint event_pk
            primary key,
    dialogid     bigint       not null,
    event        varchar(255) not null
        constraint event_event_type_fk
            references event_type,
    tidspunkt    timestamp(6),
    aktor_id     varchar(255),
    aktivitet_id varchar(255),
    lagt_inn_av  varchar(255)
);

alter table event
    owner to veilarbdialog;

create index event_indeks
    on event ((aktor_id::character varying), (lagt_inn_av::character varying));

create table feilede_kafka_aktor_id
(
    aktor_id varchar(255)                                                                            not null,
    rowid    numeric(33) default nextval('feilede_kafka_aktor_id_rowid_seq'::regclass) not null
        constraint veilarbdialog_feilede_kafka_aktor_id_pk_rowid
            primary key
);

alter table feilede_kafka_aktor_id
    owner to veilarbdialog;

alter sequence feilede_kafka_aktor_id_rowid_seq owned by feilede_kafka_aktor_id.rowid;

create table henvendelse
(
    dialog_id             bigint       not null
        constraint henvendelse_dialog_fk
            references dialog,
    sendt                 timestamp(6) not null,
    avsender_type         varchar(64)  not null,
    avsender_id           varchar(64)  not null,
    tekst                 text         not null,
    henvendelse_id        bigint       not null
        constraint henvendelse_pk
            primary key,
    kontorsperre_enhet_id varchar(255),
    viktig                smallint
);

alter table henvendelse
    owner to veilarbdialog;

create index avsendertype_sendt_id_idx
    on henvendelse (avsender_type, sendt, dialog_id);

create index henvendelse_dialog_idx
    on henvendelse (dialog_id);

create index henvendelse_sendt_idx
    on henvendelse (sendt);

create table kladd
(
    aktor_id     varchar(255)                                                            not null,
    dialog_id    bigint,
    aktivitet_id varchar(64),
    overskrift   varchar(255),
    tekst        text,
    lagt_inn_av  varchar(64)                                                             not null,
    oppdatert    timestamp(6),
    opprettet    timestamp(6) default (CURRENT_TIMESTAMP)::timestamp without time zone,
    unique_seq   bigint,
    rowid        numeric(33)  default nextval('kladd_rowid_seq'::regclass) not null
        constraint veilarbdialog_kladd_pk_rowid
            primary key
);

alter table kladd
    owner to veilarbdialog;

alter sequence kladd_rowid_seq owned by kladd.rowid;

create index kladd_idz
    on kladd (aktivitet_id, dialog_id, aktor_id, lagt_inn_av);

create table paragraf8varsel
(
    uuid        varchar(255),
    aktorid     varchar(255),
    sendt       timestamp(6),
    skalstoppes smallint,
    deaktivert  timestamp(6),
    rowid       numeric(33) default nextval('paragraf8varsel_rowid_seq'::regclass) not null
        constraint veilarbdialog_paragraf8varsel_pk_rowid
            primary key
);

alter table paragraf8varsel
    owner to veilarbdialog;

alter sequence paragraf8varsel_rowid_seq owned by paragraf8varsel.rowid;

create table siste_oppfolgingsperiode
(
    periode_uuid varchar(36)  not null
        constraint sys_c0019527
            unique,
    aktorid      varchar(20)  not null
        constraint sys_c0019526
            primary key,
    startdato    timestamp(6) not null,
    sluttdato    timestamp(6)
);

alter table siste_oppfolgingsperiode
    owner to veilarbdialog;

create table varsel
(
    aktor_id varchar(255) not null
        constraint sendt_varsel_pk
            primary key,
    sendt    timestamp(6) not null
);

alter table varsel
    owner to veilarbdialog;

create table brukernotifikasjon
(
    id                       bigint       default nextval('"iseq$$_130623"'::regclass) not null
        constraint sys_c0020075
            primary key,
    event_id                 varchar(40)                                                             not null,
    dialog_id                bigint                                                                  not null
        constraint brukernotifikasjon_fk
            references dialog,
    foedselsnummer           varchar(255)                                                            not null,
    oppfolgingsperiode_id    varchar(255)                                                            not null,
    type                     varchar(255)                                                            not null,
    status                   varchar(255)                                                            not null,
    opprettet                timestamp(6)                                                            not null,
    melding                  varchar(500)                                                            not null,
    varsel_feilet            timestamp(6),
    avsluttet                timestamp(6),
    bekreftet_sendt          timestamp(6),
    forsokt_sendt            timestamp(6),
    ferdig_behandlet         timestamp(6),
    varsel_kvittering_status varchar(255) default NULL::character varying                            not null,
    smstekst                 varchar(160),
    eposttittel              varchar(200),
    epostbody                varchar(3000),
    lenke                    varchar(255)
);

alter table brukernotifikasjon
    owner to veilarbdialog;

create unique index brukernotifikasjon_eventid_idx
    on brukernotifikasjon (event_id);

create index brukernotifikasjon_status_idx
    on brukernotifikasjon (status);

create table eskaleringsvarsel
(
    id                               numeric default nextval('"iseq$$_130634"'::regclass) not null
        constraint sys_c0020090
            primary key,
    aktor_id                         varchar(255)                                                       not null,
    opprettet_av                     varchar(255)                                                       not null,
    opprettet_dato                   timestamp(6)                                                       not null,
    tilhorende_dialog_id             numeric                                                            not null,
    tilhorende_brukernotifikasjon_id numeric,
    opprettet_begrunnelse            text,
    avsluttet_dato                   timestamp(6),
    avsluttet_av                     varchar(255),
    avsluttet_begrunnelse            text,
    gjeldende                        varchar(255)
        constraint sys_c0020469
            unique,
    constraint gjeldende
        check (((avsluttet_dato IS NULL) AND ((gjeldende)::text = (aktor_id)::text)) OR
               ((avsluttet_dato IS NOT NULL) AND (gjeldende IS NULL)))
);

alter table eskaleringsvarsel
    owner to veilarbdialog;

INSERT INTO EVENT_TYPE VALUES ('DIALOG_OPPRETTET');
INSERT INTO EVENT_TYPE VALUES ('NY_HENVENDELSE_FRA_BRUKER');
INSERT INTO EVENT_TYPE VALUES ('NY_HENVENDELSE_FRA_VEILEDER');
INSERT INTO EVENT_TYPE VALUES ('VENTER_PAA_NAV');
INSERT INTO EVENT_TYPE VALUES ('BESVART_AV_NAV');
INSERT INTO EVENT_TYPE VALUES ('VENTER_PAA_BRUKER');
INSERT INTO EVENT_TYPE VALUES ('BESVART_AV_BRUKER');
INSERT INTO EVENT_TYPE VALUES ('LEST_AV_VEILEDER');
INSERT INTO EVENT_TYPE VALUES ('LEST_AV_BRUKER');
INSERT INTO EVENT_TYPE VALUES ('SATT_TIL_HISTORISK');
