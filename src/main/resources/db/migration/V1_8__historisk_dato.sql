ALTER TABLE DIALOG ADD HISTORISK NUMBER(1);
ALTER TABLE DIALOG ADD OPPRETTET_DATO TIMESTAMP(6);

CREATE TABLE FEED_METADATA(
  TIDSPUNKT_SISTE_ENDRING TIMESTAMP(6)
);

INSERT INTO FEED_METADATA (TIDSPUNKT_SISTE_ENDRING) VALUES (CURRENT_TIMESTAMP);