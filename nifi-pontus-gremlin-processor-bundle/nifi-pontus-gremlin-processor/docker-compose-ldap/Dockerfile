
FROM alpine

RUN apk upgrade -q --no-cache

RUN apk add -q --no-cache --repository http://dl-cdn.alpinelinux.org/alpine/edge/testing \
  bash \
  ca-certificates \
  coreutils \
  curl \
  docker \
  gcc \
  gettext \
  git \
  grep \
  jq \
  libffi \
  libffi-dev \
  make \
  musl-dev \
  openssh-client \
  openssl \
  openssl-dev \
  py-pip \
  python \
  python-dev \
  sed \
  nss \
  zip 

RUN pip install -q --upgrade pip \
 && pip install -q docker-compose

VOLUME .:/work

WORKDIR /work

CMD docker-compose up

#      \
#      py-pip \
#      python-dev \
#      libffi-dev \
#      openssl-dev \
#      gcc \
#      libc-dev \
#      make
#curl -L "https://git\
#hub.com/docker/compose/releases/download/1.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

