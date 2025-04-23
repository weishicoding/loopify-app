FROM openjdk:22-jdk-slim
WORKDIR /app
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8888
# Remove the cgroup.use.v1 flag, keep container support enabled
ENV JAVA_OPTS="-XX:+UseContainerSupport -Djdk.cgroup.disabled=false"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]