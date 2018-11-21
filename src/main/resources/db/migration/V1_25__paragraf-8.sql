CREATE TABLE PARAGRAF8VARSEL(
  uuid varchar2(255),
  aktorid varchar2(255),
  sendt timestamp,
  skalStoppes number(1,0),
  deaktivert timestamp
);


alter table DIALOG add (
  ulestParagraf8Varsel number(1,0),
  paragraf8_varsel_uuid varchar2(255)
);

INSERT INTO DIALOG_EGENSKAP_TYPE(
  DIALOG_TYPE, OPPRETTET_DATO, OPPRETTET_AV, ENDRET_DATO, ENDRET_AV
) VALUES ('PARAGRAF8',CURRENT_TIMESTAMP,'KASSERT',CURRENT_TIMESTAMP,'KASSERT');
