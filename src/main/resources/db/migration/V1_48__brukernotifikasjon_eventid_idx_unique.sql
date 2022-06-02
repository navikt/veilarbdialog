DROP INDEX brukernotifikasjon_eventid_idx;
create UNIQUE index brukernotifikasjon_eventid_idx
    on BRUKERNOTIFIKASJON("EVENT_ID");