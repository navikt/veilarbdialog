
/*
select count(*) from dialog where aktor_id = '';
select count(*) from dialog_aktor where aktor_id = '';
select count(*) from eskaleringsvarsel where aktor_id = '';
select count(*) from event where aktor_id = '';
select count(*) from feilede_kafka_aktor_id where aktor_id = '';
select count(*) from kladd where aktor_id = '';
select count(*) from varsel where aktor_id = '';
select count(*) from henvendelse where avsender_id = '';
select count(*) from paragraf8varsel where aktorid = '';
select count(*) from siste_oppfolgingsperiode where aktorid = '';
*/

DECLARE
gammel VARCHAR2(20) := '';
    ny VARCHAR2(20) := '';
BEGIN
update dialog set AKTOR_ID = ny where AKTOR_ID = gammel;
update dialog_aktor set AKTOR_ID = ny where AKTOR_ID = gammel;
update eskaleringsvarsel set AKTOR_ID = ny where AKTOR_ID = gammel;
update event set AKTOR_ID = ny where AKTOR_ID = gammel;
update feilede_kafka_aktor_id set AKTOR_ID = ny where AKTOR_ID = gammel;
update kladd set AKTOR_ID = ny where AKTOR_ID = gammel;
update varsel set AKTOR_ID = ny where AKTOR_ID = gammel;
update henvendelse set AVSENDER_ID = ny where AVSENDER_ID = gammel;
update paragraf8varsel set AKTORID = ny where AKTORID = gammel;
update siste_oppfolgingsperiode set AKTORID = ny where AKTORID = gammel;
END;