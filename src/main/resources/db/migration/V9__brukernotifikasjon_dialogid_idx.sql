-- Ble kjørt med concurrently i prod :)
create index if not exists brukernotifikasjon_dialogid_idx
    ON brukernotifikasjon (dialog_id);