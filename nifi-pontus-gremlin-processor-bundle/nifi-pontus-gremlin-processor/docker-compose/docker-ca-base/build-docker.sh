#!/bin/bash

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

cd ${DIR}/docker 
docker build --rm --no-cache -t pontusvisiongdpr/pontus-ca-base  .
docker push pontusvisiongdpr/pontus-ca-base
