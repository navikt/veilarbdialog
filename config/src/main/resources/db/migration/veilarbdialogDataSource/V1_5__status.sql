ALTER TABLE DIALOG RENAME COLUMN lest_av_bruker TO lest_av_bruker_tid;
ALTER TABLE DIALOG RENAME COLUMN lest_av_veileder TO lest_av_veileder_tid;

alter table DIALOG add siste_status_endring timestamp not null;
alter table DIALOG add skal_vente_pa_svar number(1, 0) not null;
alter table DIALOG add markert_som_ferdigbehandlet number(1, 0) not null;

CREATE VIEW DIALOG_STATUS AS (
  SELECT
    d.dialog_id,
    aktor_id,
    CASE WHEN lest_av_bruker_tid >= NVL(MAX(sendt), lest_av_bruker_tid) THEN 1 ELSE 0 END as lest_av_bruker,
    CASE WHEN lest_av_veileder_tid >= NVL(MAX(sendt), lest_av_veileder_tid) THEN 1 ELSE 0 END as lest_av_veileder,
    CASE WHEN skal_vente_pa_svar > 0 AND siste_status_endring >= NVL(MAX(sendt), siste_status_endring) THEN 1 ELSE 0 END as venter_pa_svar,
    CASE WHEN markert_som_ferdigbehandlet > 0 AND siste_status_endring >= NVL(MAX(sendt), siste_status_endring) THEN 1 ELSE 0 END as ferdigbehandlet,
    GREATEST(siste_status_endring, NVL(MAX(sendt), siste_status_endring)) as siste_endring
  FROM DIALOG d
  LEFT JOIN HENVENDELSE h ON (d.DIALOG_ID = h.DIALOG_ID AND h.avsender_type = 'BRUKER')
  GROUP BY d.dialog_id, aktor_id, lest_av_bruker_tid, lest_av_veileder_tid, markert_som_ferdigbehandlet, skal_vente_pa_svar, siste_status_endring
);

CREATE VIEW AKTOR_STATUS AS (
  SELECT AKTOR_ID,
    MAX(siste_endring) as siste_endring,
    MIN(ferdigbehandlet) as ferdigbehandlet,
    MAX(venter_pa_svar) as venter_pa_svar
  FROM DIALOG_STATUS
  GROUP BY AKTOR_ID
);
