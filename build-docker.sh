#!/bin/bash
set -e
DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd $DIR/
TAG=${TAG:-latest}
docker build --no-cache --rm . -t pontusvisiongdpr/pontus-nifi-gremlin-lib:${TAG}

docker push pontusvisiongdpr/pontus-nifi-gremlin-lib:${TAG}

