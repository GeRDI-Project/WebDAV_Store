FROM openjdk:8
COPY ./target/WebDAV-Store-Service-0.0-SNAPSHOT-jar-with-dependencies.jar /usr/src/store-prototype/app.jar
WORKDIR /usr/src/store-prototype
ENTRYPOINT ["java", "-jar" , "app.jar"]