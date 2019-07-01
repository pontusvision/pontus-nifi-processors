#!/bin/bash -x

export tls_home=${tls_home:-/etc/pki}
export tls_rootca_passphrase=${tls_rootca_passphrase:-pa55word}
export privkey_passphrase=${privkey_passphrase:-pa55word}
export truststore_pass=${truststore_pass:-changeit}
export truststore_type=${truststore_type:-JKS}
export tls_host_name=${tls_host_name:-$(hostname -s)}

mkdir -p ${tls_home}/private ${tls_home}/java

if [[ ! -f ${tls_home}/private/root-ca.crt ]]; then
  /create-ca.sh
fi

openssl genrsa -out "${tls_home}/private/${tls_host_name}.pem" -passout "pass:${privkey_passphrase}" 4096

export IPADDR=$(hostname -i)
export FULL_HOST_NAME=$(hostname -f)
cat << EOF2 >> ${tls_home}/private/${tls_host_name}.cnf
[server]
authorityKeyIdentifier=keyid,issuer
basicConstraints = critical,CA:FALSE
extendedKeyUsage=serverAuth,clientAuth
keyUsage = critical, digitalSignature, keyEncipherment
subjectAltName = DNS:${FULL_HOST_NAME}, IP:${IPADDR}
subjectKeyIdentifier=hash
EOF2

openssl req -new -key "${tls_home}/private/${tls_host_name}.pem" -out "${tls_home}/private/${tls_host_name}.csr" -sha256 -subj "/C=UK/ST=London/L=London/O=PontusVision/OU=GDPR/CN=${tls_host_name}"

openssl x509 -req -days 750 -passin "pass:${tls_rootca_passphrase}" -in "${tls_home}/private/${tls_host_name}.csr" -sha256 -CA "${tls_home}/private/root-ca.crt" -CAkey "${tls_home}/private/root-ca.pem"  -CAcreateserial -out "${tls_home}/private/${tls_host_name}.crt" -extfile "${tls_home}/private/${tls_host_name}.cnf" -extensions server

openssl pkcs12 -export -in "${tls_home}/private/${tls_host_name}.crt" -inkey "${tls_home}/private/${tls_host_name}.pem" -out  "${tls_home}/private/${tls_host_name}.p12" -name "${tls_host_name}" -password "pass:${privkey_passphrase}"

keytool -importkeystore -srckeystore "${tls_home}/private/${tls_host_name}.p12" -destkeystore "${tls_home}/java/keystore.jks" -srcstorepass "${privkey_passphrase}" -deststorepass "${privkey_passphrase}"
openssl pkcs8 -in ${tls_home}/private/${tls_host_name}.pem -topk8 -nocrypt -out ${tls_home}/private/${tls_host_name}.pem.pk8

keytool -keystore "${tls_home}/java/truststore.jks" -alias ${tls_host_name} -import -file "${tls_home}/private/${tls_host_name}.crt" -storetype "${truststore_type}" -storepass "${truststore_pass}" -noprompt

