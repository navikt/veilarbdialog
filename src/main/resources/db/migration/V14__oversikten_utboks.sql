create table oversikten_forsending (
    uuid                         uuid            not null,
    fnr                          varchar(11)     not null,
    opprettet                    timestamp       not null,
    tidspunkt_sendt              timestamp,
    utsending_status             text            not null,
    melding                      json            not null,
    kategori                     text            not null
)