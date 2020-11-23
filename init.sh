SERVICEUSER_USERNAME=$(cat /secrets/serviceuser/username)
SERVICEUSER_PASSWORD=$(cat /secrets/serviceuser/password)
JDBC_URL=$(cat /var/run/secrets/nais.io/oracle_config/jdbc_url)
JDBC_USERNAME=$(cat /var/run/secrets/nais.io/oracle_creds/username)
JDBC_PASSWORD=$(cat /var/run/secrets/nais.io/oracle_creds/password)

export SERVICEUSER_USERNAME
export SERVICEUSER_PASSWORD
export JDBC_URL
export JDBC_USERNAME
export JDBC_PASSWORD