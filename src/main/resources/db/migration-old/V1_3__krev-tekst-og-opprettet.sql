/*
Obs obs: kan ikke modifisere clobber

SQL Error: ORA-22859: ugyldig endring av kolonner
22859. 00000 -  "invalid modification of columns"
*Cause:    An attempt was made to modify an object, REF, VARRAY, nested
           table, or LOB column type.
*Action:   Create a new column of the desired type and copy the current
           column data to the new type using the appropriate type
           constructor.

kjører enkel variant siden vi ikke bryr oss om dataene nå, men ellers bør vi bruke expand and contract!
 */
TRUNCATE TABLE HENVENDELSE;
alter table HENVENDELSE DROP COLUMN tekst;
alter table HENVENDELSE add tekst clob not null;


alter table HENVENDELSE modify sendt timestamp not null;