-- Move all arenaid to correct column
UPDATE DIALOG set ARENA_ID = AKTIVITET_ID where AKTIVITET_ID like 'ARENA%';
-- Overwrite all aktivitet-id that has an arena-id and is migrated with functional-id
merge into DIALOG d
using ID_MAPPINGER@VEILARBAKTIVITET idm on (d.ARENA_ID = idm.EKSTERN_REFERANSE_ID)
when matched then
    update set d.AKTIVITET_ID = idm.AKTIVITET_ID;