export SERVICEUSER_USERNAME=$(cat /secrets/serviceuser/username)
export SERVICEUSER_PASSWORD=$(cat /secrets/serviceuser/password)
export VEILARBDIALOGDATASOURCE_USERNAME=$(cat /var/run/secrets/nais.io/oracle_creds/username)
export VEILARBDIALOGDATASOURCE_PASSWORD=$(cat /var/run/secrets/nais.io/oracle_creds/password)