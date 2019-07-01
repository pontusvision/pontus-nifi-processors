#!/bin/bash -x

export tls_home=${tls_home:-/etc/pki}
export tls_rootca_passphrase=${tls_rootca_passphrase:-pa55word}
export privkey_passphrase=${privkey_passphrase:-pa55word}
export truststore_pass=${truststore_pass:-changeit}
export truststore_type=${truststore_type:-JKS}

mkdir -p ${tls_home}/private ${tls_home}/java

cat << 'EOF2' >> ${tls_home}/private/root-ca.cnf
[root_ca]
basicConstraints = critical,CA:TRUE,pathlen:1
keyUsage = critical, nonRepudiation, cRLSign, keyCertSign
subjectKeyIdentifier=hash
EOF2

openssl genrsa -out "${tls_home}/private/root-ca.pem" -passout "pass:${tls_rootca_passphrase}" 4096

openssl req -new -key "${tls_home}/private/root-ca.pem" -out "${tls_home}/private/root-ca.csr" -sha256 -subj '/C=UK/ST=London/L=London/O=PontusVision/OU=GDPR/CN=CA'

openssl x509 -req  -days 3650  -in "${tls_home}/private/root-ca.csr" -signkey "${tls_home}/private/root-ca.pem" -sha256 -out "${tls_home}/private/root-ca.crt" -extfile "${tls_home}/private/root-ca.cnf" -extensions root_ca

keytool -keystore "${tls_home}/java/truststore.jks" -alias CARoot -import -file "${tls_home}/private/root-ca.crt" -storetype "${truststore_type}" -storepass "${truststore_pass}" -noprompt
keytool -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts -alias PontusCARoot -import -file "${tls_home}/private/root-ca.crt" -storetype "${truststore_type}" -storepass "changeit" -noprompt

