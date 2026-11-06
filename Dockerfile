FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21

ENV TZ="Europe/Oslo"
ENV APP_JAR=veilarbdialog.jar
WORKDIR /app
COPY target/veilarbdialog.jar ./
EXPOSE 8080
USER nonroot
ENTRYPOINT ["java", "-jar", "veilarbdialog.jar"]
