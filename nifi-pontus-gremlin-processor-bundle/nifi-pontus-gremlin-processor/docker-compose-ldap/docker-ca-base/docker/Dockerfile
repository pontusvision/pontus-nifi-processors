FROM alpine as builder
RUN apk upgrade -q --no-cache
RUN apk add -q --no-cache \
      bash \
      gettext \
      openjdk8-jre-base \
      openssl \
      unzip \
      zip 

RUN mkdir /media/storage && \
    chmod 0777 /media/storage && \
    mkdir -p /etc/samba


COPY ./create-ca.sh /create-ca.sh
COPY ./create-keys.sh /create-keys.sh

RUN /create-ca.sh

