CREATE TABLE paragraf8varsel(
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
