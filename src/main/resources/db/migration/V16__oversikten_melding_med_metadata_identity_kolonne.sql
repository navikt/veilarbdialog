drop table oversikten_melding_med_metadata;
CREATE TYPE OVERSIKTEN_UTSENDING_STATUS AS ENUM ('SKAL_STARTES', 'SKAL_STOPPES', 'STARTET', 'STOPPET', 'ABORTERT');
create table oversikten_melding_med_metadata (
                                                 melding_key                  uuid not null,
                                                 fnr                          varchar(11)     not null,
                                                 opprettet                    timestamp       not null,
                                                 tidspunkt_startet              timestamp,
                                                 tidspunkt_stoppet              timestamp,
                                                 utsending_status             OVERSIKTEN_UTSENDING_STATUS  not null,
                                                 melding                      json            not null,
                                                 kategori                     text            not null
);
create unique index oversikten_melding_med_metadata_melding_key_pk on oversikten_melding_med_metadata(melding_key);
alter table oversikten_melding_med_metadata add primary key using index oversikten_melding_med_metadata_melding_key_pk;
create index oversikten_melding_med_metadata_utsending_status_idx on oversikten_melding_med_metadata(utsending_status);
