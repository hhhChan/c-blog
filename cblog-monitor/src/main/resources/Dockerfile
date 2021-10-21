FROM java:alpine
VOLUME /tmp
ADD cblog-monitor-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Xms256m","-Xmx256m","-jar","/app.jar"]
