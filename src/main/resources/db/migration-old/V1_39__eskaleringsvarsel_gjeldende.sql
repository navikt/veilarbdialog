CREATE TABLE ESKALERINGSVARSEL_GJELDENDE
(
    AKTOR_ID NVARCHAR2(255) NOT NULL UNIQUE,
    VARSEL_ID NUMBER REFERENCES ESKALERINGSVARSEL(ID)
);