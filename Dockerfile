FROM ghcr.io/navikt/poao-baseimages/java:17

COPY nais/init.sh /init-scripts/init.sh
COPY /target/veilarbdialog.jar app.jar
