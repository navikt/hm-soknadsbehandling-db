FROM gcr.io/distroless/java21-debian12:nonroot
COPY app/build/libs/app-all.jar /app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["/app.jar"]
