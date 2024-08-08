-- Move all arenaid to correct column
UPDATE DIALOG set ARENA_ID = AKTIVITET_ID where AKTIVITET_ID like 'ARENA%';