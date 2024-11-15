
create table min_side_varsel
(
    varsel_id                uuid               not null primary key ,
    foedselsnummer           varchar(255)                                              not null,
    oppfolgingsperiode_id    varchar(255)                                              not null,
    type                     varchar(255)                                              not null,
    status                   varchar(255)                                              not null,
    varsel_kvittering_status varchar(255) default NULL::character varying              not null,
    opprettet                timestamp(6)                                              not null,
    varsel_feilet            timestamp(6),
    avsluttet                timestamp(6),
    bekreftet_sendt          timestamp(6),
    forsokt_sendt            timestamp(6),
    ferdig_behandlet         timestamp(6),
    melding                  varchar(500)                                              not null,
    lenke                    varchar(255),
    skal_batches             boolean      default false                                not null
);


create unique index brukernotifikasjon_varsel_id
    on min_side_varsel (varsel_id);

create index bmin_side_varsel_status_idx
    on min_side_varsel (status);

-- grant select on min_side_varsel to datastream;

create table min_side_varsel_dialog_mapping (
    varsel_id uuid references min_side_varsel(varsel_id),
    dialog_id bigint references dialog(dialog_id)
);

-- grant select on min_side_varsel_dialog_mapping to datastream;

