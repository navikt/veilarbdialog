UPDATE DIALOG set OPPDATERT = CURRENT_TIMESTAMP where OPPDATERT is NULL;
ALTER TABLE DIALOG MODIFY OPPDATERT TIMESTAMP NOT NULL;
