FROM busybox:1.36.1-uclibc as busybox

FROM gcr.io/distroless/java21

COPY --from=busybox /bin/sh /bin/sh
COPY --from=busybox /bin/cat /bin/cat
COPY --from=busybox /bin/printenv /bin/printenv
COPY --from=busybox /bin/mkdir /bin/mkdir
COPY --from=busybox /bin/chown /bin/chown

ENV TZ="Europe/Oslo"
ENV APP_JAR=veilarbdialog.jar
WORKDIR /app
COPY target/veilarbdialog.jar ./
RUN /bin/mkdir /secure-logs
RUN chown nonroot /secure-logs
EXPOSE 8080
USER nonroot
ENTRYPOINT set -x \
    && exec java \
      ${DEFAULT_JVM_OPTS} \
      ${JAVA_OPTS} \
      -jar ${APP_JAR} \
      ${RUNTIME_OPTS}
