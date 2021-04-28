FROM pontusvisiongdpr/pontus-graphdb-lib as graphdblib
FROM maven:3.6-jdk-8-alpine as builder
COPY --from=graphdblib /root/.m2/ /root/.m2/
RUN apk add git
COPY nifi-pontus-gremlin-processor-bundle/nifi-pontus-gremlin-processor/pom.xml /pontus-nifi-processors/nifi-pontus-gremlin-processor-bundle/nifi-pontus-gremlin-processor/pom.xml
COPY nifi-pontus-gremlin-processor-bundle/nifi-pontus-gremlin-processor-nar/pom.xml /pontus-nifi-processors/nifi-pontus-gremlin-processor-bundle/nifi-pontus-gremlin-processor-nar/pom.xml
COPY nifi-pontus-gremlin-processor-bundle/pom.xml /pontus-nifi-processors/nifi-pontus-gremlin-processor-bundle/pom.xml
COPY nifi-pontus-msoffice-processor-bundle/nifi-pontus-msoffice-processor/pom.xml /pontus-nifi-processors/nifi-pontus-msoffice-processor-bundle/nifi-pontus-msoffice-processor/pom.xml
COPY nifi-pontus-msoffice-processor-bundle/nifi-pontus-msoffice-processor-nar/pom.xml /pontus-nifi-processors/nifi-pontus-msoffice-processor-bundle/nifi-pontus-msoffice-processor-nar/pom.xml
COPY nifi-pontus-msoffice-processor-bundle/pom.xml /pontus-nifi-processors/nifi-pontus-msoffice-processor-bundle/pom.xml
COPY pom.xml /pontus-nifi-processors/pom.xml
COPY pontus-gdpr-graph/pom.xml /pontus-nifi-processors/pontus-gdpr-graph/pom.xml

RUN cd /pontus-nifi-processors && \
    mvn -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B verify --fail-never


COPY . /pontus-nifi-processors
RUN cd /pontus-nifi-processors && \
    mvn -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B clean package -U -DskipTests 

FROM scratch
COPY --from=builder /pontus-nifi-processors/*/*/target/*.nar /opt/nifi/nifi-current/lib/
