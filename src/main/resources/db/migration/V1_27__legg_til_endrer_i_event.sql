alter table EVENT add (
  lagt_inn_av NVARCHAR2(255)
);

CREATE OR REPLACE VIEW DVH_Dialog_hendelse AS (
  SELECT
    event_id,
    dialogid,
    event,
    tidspunkt,
    aktor_id,
    aktivitet_id,
    lagt_inn_av
  FROM EVENT;