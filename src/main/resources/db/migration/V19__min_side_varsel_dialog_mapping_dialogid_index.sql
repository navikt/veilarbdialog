-- flyway:nonTransactional
create index concurrently if not exists min_side_varsel_dialog_mapping_dialog_id_index
    on min_side_varsel_dialog_mapping (dialog_id);