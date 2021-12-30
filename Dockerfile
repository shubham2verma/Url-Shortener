FROM maven:3.6.0-jdk-11-slim

ADD app /target/UrlShortenerProject	
WORKDIR "/target/UrlShortenerProject"
RUN mvn clean install

CMD ["java", "-jar", "./target/UrlShortenerProject-0.0.1-SNAPSHOT.jar"]