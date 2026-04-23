FROM eclipse-temurin:17-jdk

# 세종대 포털 SSL 호환성: SHA1 알고리즘 제한 해제
RUN sed -i 's/SHA1 jdkCA & usage TLSServer, //g' /opt/java/openjdk/conf/security/java.security && \
    sed -i 's/SHA1 usage SignedJAR & denyAfter 2019-01-01, //g' /opt/java/openjdk/conf/security/java.security

COPY ./build/libs/*SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]