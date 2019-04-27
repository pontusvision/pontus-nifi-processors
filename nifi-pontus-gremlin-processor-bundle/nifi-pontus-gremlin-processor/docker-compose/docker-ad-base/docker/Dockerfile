FROM pontusvisiongdpr/pontus-ca-base
RUN apk upgrade -q --no-cache
RUN apk add -q --no-cache \
      krb5 \
      samba \
      samba-dc 

RUN mkdir -p /etc/samba

COPY ./pre-config-samba.sh /pre-config-samba.sh
COPY ./post-config-samba.sh /post-config-samba.sh
COPY ./init.sh /init.sh
COPY ./readiness.sh /readiness.sh

ENTRYPOINT /init.sh
