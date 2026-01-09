-- flyway:nonTransactional
create index concurrently min_side_varsel_dialog_mapping_dialog_id_index
    on min_side_varsel_dialog_mapping (dialog_id);