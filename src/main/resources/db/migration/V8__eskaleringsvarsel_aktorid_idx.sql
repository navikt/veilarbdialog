-- Ble kjørt med concurrently i prod :)
create index if not exists eskaleringsvarsel_aktorid_idx
    ON eskaleringsvarsel (aktor_id);
