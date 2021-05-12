delete from HENVENDELSE where (select 1 from dual) = 1;

delete from DIALOG_AKTOR where (select 1 from dual) = 1;
delete from DIALOG_EGENSKAP where (select 1 from dual) = 1;
delete from DIALOG where (select 1 from dual) = 1;
delete from EVENT;
