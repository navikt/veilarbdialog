-- If this field is not NULL, the information in the row should be
-- limited to only the users belonging to this specific unit.
ALTER TABLE DIALOG ADD KONTORSPERRE_ENHET_ID NVARCHAR2(255) NULL;
ALTER TABLE HENVENDELSE ADD KONTORSPERRE_ENHET_ID NVARCHAR2(255) NULL;
