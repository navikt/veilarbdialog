create table oversikten_vaas_utboks (
    fnr                          varchar(11)     not null,
    opprettet                    timestamp       not null,
    tidspunkt_sendt              timestamp       not null,
    utsending_status             text            not null,
    melding                      json            not null
)