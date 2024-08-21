drop index veilarbdialog.dialog_oppfolgingsperiode_idx;

create index dialog_oppfolgingsperiode_idx
    on veilarbdialog.dialog (oppfolgingsperiode_uuid);