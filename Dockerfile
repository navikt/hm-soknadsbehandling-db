FROM gcr.io/distroless/java21-debian12:nonroot
COPY build/libs/hm-soknadsbehandling-db-all.jar /app.jar
ENV TZ="Europe/Oslo"
CMD ["/app.jar"]
