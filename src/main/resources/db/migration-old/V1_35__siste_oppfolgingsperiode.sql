create table siste_oppfolgingsperiode (
    periode_uuid varchar(36) not null unique,
    aktorId varchar(20) not null primary key,
    startdato timestamp not null,
    sluttdato timestamp
);