CREATE TABLE EKSTERN_VARSEL_KVITTERING
(
    TIDSPUNKT TIMESTAMP not null,
    BRUKERNOTIFIKASJON_BESTILLING_ID varchar2(255) not null,
    DOKNOTIFIKASJON_STATUS varchar2(255) not null,
    MELDING varchar2(255) not null,
    DISTRIBUSJON_ID number,
    JSON_PAYLOAD clob not null
);