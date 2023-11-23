FROM gcr.io/distroless/java17-debian12:latest
COPY /build/libs/hm-soknadsbehandling-db.jar /app.jar
ENV TZ="Europe/Oslo"
CMD ["/app.jar"]
