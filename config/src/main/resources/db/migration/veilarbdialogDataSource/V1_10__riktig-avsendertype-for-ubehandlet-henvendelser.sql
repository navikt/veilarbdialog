CREATE OR REPLACE VIEW DIALOG_STATUS AS (
  SELECT
    d.dialog_id,
    aktor_id,
    CASE WHEN lest_av_bruker_tid >= NVL(MAX(h.sendt), lest_av_bruker_tid) THEN 1 ELSE 0 END as lest_av_bruker,
    CASE WHEN lest_av_veileder_tid >= NVL(MAX(h.sendt), lest_av_veileder_tid) THEN 1 ELSE 0 END as lest_av_veileder,
    CASE WHEN siste_vente_pa_svar_tid >= NVL(MAX(bruker_henvendelser.sendt), siste_vente_pa_svar_tid) THEN siste_vente_pa_svar_tid ELSE NULL END as vente_pa_svar_tid,
    MIN (ubehandlet_henvendelser.sendt) as eldste_ubehandlede_tid,
    GREATEST(siste_status_endring, NVL(MAX(h.sendt), siste_status_endring)) as siste_endring
  FROM DIALOG d
  LEFT JOIN HENVENDELSE h ON d.DIALOG_ID = h.DIALOG_ID
  LEFT JOIN HENVENDELSE bruker_henvendelser ON
    d.DIALOG_ID = bruker_henvendelser.DIALOG_ID AND
    bruker_henvendelser.avsender_type = 'BRUKER'
  LEFT JOIN HENVENDELSE ubehandlet_henvendelser ON
    d.DIALOG_ID = ubehandlet_henvendelser.DIALOG_ID AND
    ubehandlet_henvendelser.avsender_type = 'BRUKER' AND
    (siste_ferdigbehandlet_tid IS NULL OR ubehandlet_henvendelser.sendt > siste_ferdigbehandlet_tid)
  GROUP BY d.dialog_id, aktor_id, lest_av_bruker_tid, lest_av_veileder_tid, siste_ferdigbehandlet_tid, siste_vente_pa_svar_tid, siste_status_endring
);