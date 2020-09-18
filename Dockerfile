FROM navikt/pus-nais-java-app

COPY /target/veilarbdialog /app
COPY init.sh /init-scripts/init.sh