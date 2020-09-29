FROM navikt/pus-nais-java-app

COPY /target/veilarbdialog*.jar /app
COPY init.sh /init-scripts/init.sh