# syntax = docker/dockerfile:1.2

#############
### build ###
#############

FROM maven:3.9.16-amazoncorretto-25 as build

WORKDIR /app

COPY . .

ENV MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=WARN -Dorg.slf4j.simpleLogger.showDateTime=true -Djava.awt.headless=true"
ENV MAVEN_CLI_OPTS="-B -U --batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true"

RUN --mount=type=cache,target=/root/.m2/repository mvn $MAVEN_CLI_OPTS clean install -Dmaven.test.skip=true -DskipTests

############
### prod ###
############

FROM amazoncorretto:25
WORKDIR /app
EXPOSE 8080
COPY --from=build /app/epub-generator/target/epub-generator-exec.jar /app/epub-generator.jar

CMD ["java", "-jar", "epub-generator.jar"]
