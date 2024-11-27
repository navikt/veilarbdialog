create table oversikten_forsending (
    melding_key                  uuid            not null,
    fnr                          varchar(11)     not null,
    opprettet                    timestamp       not null,
    tidspunkt_sendt              timestamp,
    utsending_status             text            not null,
    melding                      json            not null,
    kategori                     text            not null
);
create index oversikten_forsending_melding_key_idx on oversikten_forsending(melding_key);