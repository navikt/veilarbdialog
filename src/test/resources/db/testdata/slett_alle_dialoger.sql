delete from HENVENDELSE where (select 1 from dual) = 1;
delete from ESKALERINGSVARSEL_GJELDENDE where (select 1 from dual) = 1;
delete from ESKALERINGSVARSEL where (select 1 from dual) = 1;
delete from BRUKERNOTIFIKASJON where (select 1 from dual) = 1;

delete from DIALOG_AKTOR where (select 1 from dual) = 1;
delete from DIALOG_EGENSKAP where (select 1 from dual) = 1;
delete from DIALOG where (select 1 from dual) = 1;
delete from EVENT;
delete from VARSEL;
