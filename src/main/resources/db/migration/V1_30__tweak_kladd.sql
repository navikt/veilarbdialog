ALTER TABLE KLADD
ADD UNIQUE_SEQ NUMBER(19, 0);

create sequence KLADD_ID_SEQ start with 1 increment by 1;

UPDATE KLADD SET UNIQUE_SEQ = 0;

