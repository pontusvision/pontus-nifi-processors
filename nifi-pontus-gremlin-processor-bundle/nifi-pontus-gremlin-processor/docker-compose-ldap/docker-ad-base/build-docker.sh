#!/bin/bash

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

cd ${DIR}/docker 
docker build --rm --no-cache --add-host pontus-sandbox.pontusvision.com:172.17.0.2 -t pontusvisiongdpr/pontus-ad-base  .
docker push pontusvisiongdpr/pontus-ad-base
#docker run --privileged --hostname pontus-sandbox.pontusvision.com -d --rm -p389:389/udp -p389:389 -p636:636 pontusvisiongdpr/pontus-ad-base
