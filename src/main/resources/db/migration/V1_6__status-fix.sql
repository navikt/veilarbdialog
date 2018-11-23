
CREATE OR REPLACE VIEW DIALOG_STATUS AS (
  SELECT
    d.dialog_id,
    aktor_id,
    CASE WHEN lest_av_bruker_tid >= NVL(MAX(h.sendt), lest_av_bruker_tid) THEN 1 ELSE 0 END as lest_av_bruker,
    CASE WHEN lest_av_veileder_tid >= NVL(MAX(h.sendt), lest_av_veileder_tid) THEN 1 ELSE 0 END as lest_av_veileder,
    CASE WHEN skal_vente_pa_svar > 0 AND siste_status_endring >= NVL(MAX(bh.sendt), siste_status_endring) THEN 1 ELSE 0 END as venter_pa_svar,
    CASE WHEN markert_som_ferdigbehandlet > 0 AND siste_status_endring >= NVL(MAX(bh.sendt), siste_status_endring) THEN 1 ELSE 0 END as ferdigbehandlet,
    GREATEST(siste_status_endring, NVL(MAX(h.sendt), siste_status_endring)) as siste_endring
  FROM DIALOG d
  LEFT JOIN HENVENDELSE bh ON (d.DIALOG_ID = bh.DIALOG_ID AND bh.avsender_type = 'BRUKER')
  LEFT JOIN HENVENDELSE h ON d.DIALOG_ID = h.DIALOG_ID
  GROUP BY d.dialog_id, aktor_id, lest_av_bruker_tid, lest_av_veileder_tid, markert_som_ferdigbehandlet, skal_vente_pa_svar, siste_status_endring
);

