#!/bin/bash

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

cd ${DIR}/docker 
docker build  -t pontusvisiongdpr/pontus-nifi-test-ad-base  .
docker push pontusvisiongdpr/pontus-nifi-test-ad-base
#docker run --privileged --hostname pontus-sandbox.pontusvision.com -d --rm -p389:389/udp -p389:389 -p636:636 pontusvisiongdpr/pontus-nifi-test-ad-base
