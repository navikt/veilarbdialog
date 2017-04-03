create table DIALOG (
    dialog_id number(19, 0) not null,
    aktor_id varchar(255),
    overskrift varchar(255),
    constraint DIALOG_PK primary key (dialog_id)
);
create sequence DIALOG_ID_SEQ start with 1 increment by 1;
CREATE INDEX DIALOG_AKTOR_IDX ON DIALOG(aktor_id);


create table HENVENDELSE (
    dialog_id number(19, 0) not null,
    sendt timestamp,
    tekst clob,
    CONSTRAINT HENVENDELSE_DIALOG_FK FOREIGN KEY (dialog_id) REFERENCES DIALOG (dialog_id)
);
CREATE INDEX HENVENDELSE_DIALOG_IDX ON HENVENDELSE(dialog_id);