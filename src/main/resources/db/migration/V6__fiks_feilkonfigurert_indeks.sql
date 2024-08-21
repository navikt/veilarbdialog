drop index dialog_oppfolgingsperiode_idx;

create index dialog_oppfolgingsperiode_idx
    on dialog (oppfolgingsperiode_uuid);