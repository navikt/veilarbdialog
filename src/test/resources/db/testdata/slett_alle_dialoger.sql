delete from HENVENDELSE where (select 1) = 1;
delete from ESKALERINGSVARSEL where (select 1) = 1;
delete from BRUKERNOTIFIKASJON where (select 1) = 1;


delete from DIALOG_EGENSKAP where (select 1) = 1;
delete from min_side_varsel_dialog_mapping where (select 1) = 1;
delete from min_side_varsel where (select 1) = 1;
delete from DIALOG where (select 1) = 1;
delete from EVENT;
delete from VARSEL;
delete from KLADD;
