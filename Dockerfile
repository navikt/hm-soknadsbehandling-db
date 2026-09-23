FROM gcr.io/distroless/java25-debian13:nonroot
WORKDIR /app
COPY app/build/libs/app-all.jar app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["./app.jar"]
