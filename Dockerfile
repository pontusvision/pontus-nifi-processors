FROM pontusvisiongdpr/pontus-graphdb-lib as graphdblib
FROM maven:3.6-jdk-8-alpine as builder
COPY --from=graphdblib /root/.m2/ /root/.m2/
COPY . /pontus-nifi-processors
RUN cd pontus-nifi-processors && \
    mvn clean package -U -DskipTests

FROM scratch
COPY --from=builder /pontus-nifi-processors/*/*/target/*.nar /opt/nifi/nifi-current/lib/
