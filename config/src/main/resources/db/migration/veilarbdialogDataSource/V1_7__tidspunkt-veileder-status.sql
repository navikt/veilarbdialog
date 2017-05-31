DROP VIEW AKTOR_STATUS;
DROP VIEW DIALOG_STATUS;

ALTER TABLE DIALOG DROP skal_vente_pa_svar;
ALTER TABLE DIALOG DROP markert_som_ferdigbehandlet;

ALTER TABLE DIALOG ADD siste_vente_pa_svar_tid TIMESTAMP;
ALTER TABLE DIALOG ADD siste_ferdigbehandlet_tid TIMESTAMP;

CREATE VIEW DIALOG_STATUS AS (
  SELECT
    d.dialog_id,
    aktor_id,
    CASE WHEN lest_av_bruker_tid >= NVL(MAX(h.sendt), lest_av_bruker_tid) THEN 1 ELSE 0 END as lest_av_bruker,
    CASE WHEN lest_av_veileder_tid >= NVL(MAX(h.sendt), lest_av_veileder_tid) THEN 1 ELSE 0 END as lest_av_veileder,
    CASE WHEN siste_vente_pa_svar_tid >= NVL(MAX(bruker_henvendelse.sendt), siste_vente_pa_svar_tid) THEN siste_vente_pa_svar_tid ELSE NULL END as vente_pa_svar_tid,
    MIN (ubehandlet_henvendelse.sendt) as eldste_ubehandlede_tid,
    GREATEST(siste_status_endring, NVL(MAX(h.sendt), siste_status_endring)) as siste_endring
  FROM DIALOG d
  LEFT JOIN HENVENDELSE h ON d.DIALOG_ID = h.DIALOG_ID
  LEFT JOIN HENVENDELSE bruker_henvendelse ON (d.DIALOG_ID = bruker_henvendelse.DIALOG_ID AND bruker_henvendelse.avsender_type = 'BRUKER')
  LEFT JOIN HENVENDELSE ubehandlet_henvendelse ON
    (d.DIALOG_ID = ubehandlet_henvendelse.DIALOG_ID AND (siste_ferdigbehandlet_tid IS NULL OR ubehandlet_henvendelse.sendt > siste_ferdigbehandlet_tid))
  GROUP BY d.dialog_id, aktor_id, lest_av_bruker_tid, lest_av_veileder_tid, siste_ferdigbehandlet_tid, siste_vente_pa_svar_tid, siste_status_endring
);

CREATE VIEW AKTOR_STATUS AS (
  SELECT AKTOR_ID,
    MAX(siste_endring) as siste_endring,
    MIN(vente_pa_svar_tid) as tidspunkt_eldste_ventende,
    MIN(eldste_ubehandlede_tid) as tidspunkt_eldste_ubehandlede
  FROM DIALOG_STATUS
  GROUP BY AKTOR_ID
);

