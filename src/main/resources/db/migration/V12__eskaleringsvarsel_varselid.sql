alter table eskaleringsvarsel
    add column tilhorende_minside_varsel UUID references min_side_varsel(varsel_id);
