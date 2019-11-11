# This file is mostly taken from .. https://www.callicoder.com/spring-boot-docker-example/

# Start with a base image containing Java runtime
FROM anapsix/alpine-java

# Add Maintainer Info
LABEL maintainer="your@email.com"
LABEL application="your-app-name"

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Args to pass in at build image time
# docker build --file env/your-app-name-prod/Dockerfile --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') --build-arg SPRING_PROFILE=your-app-name-prod -t your-app-name-prod .
ARG BUILD_DATE
ARG SPRING_PROFILE
ENV SPRING_PROFILE_ENV=$SPRING_PROFILE

LABEL org.label-schema.schema-version="1.0"
LABEL org.label-schema.build-date=$BUILD_DATE

# The application's jar file
ARG JAR_FILE=build/libs/rckt_snooze_spring-0.0.1.jar

# Add the application's jar to the container
ADD ${JAR_FILE} your-app-name.jar

# Run the jar file 
ENTRYPOINT [ "sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILE_ENV} -Djava.security.egd=file:/dev/./urandom -jar /your-app-name.jar" ]