alter table min_side_varsel drop column varsel_feilet;
alter table min_side_varsel drop column avsluttet;
alter table min_side_varsel drop column bekreftet_sendt;
alter table min_side_varsel drop column forsokt_sendt;
alter table min_side_varsel drop column ferdig_behandlet;
alter table min_side_varsel add column oppdatert timestamp(6) default current_timestamp;