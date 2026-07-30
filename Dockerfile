FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle war --no-daemon

FROM tomcat:9.0-jdk17
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/build/libs/anchack-backend.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]