package dev.ciprian;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class S3EventUploadHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static String BUCKET_NAME;

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final Faker faker;

    public S3EventUploadHandler() {
        this.s3Client = S3Client.builder()
                .region(Region.EU_CENTRAL_1)
                .build();

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.faker = new Faker();

        BUCKET_NAME = System.getenv("BUCKET_NAME");
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent requestEvent, Context context) {
        var lambdaLogger = context.getLogger();

        lambdaLogger.log("Received upload request: %s%n".formatted(requestEvent));

        if (BUCKET_NAME == null) {
            return buildHttpResponse(emptyList(), "Bucket name is not configured", HttpStatusCode.INTERNAL_SERVER_ERROR, false);
        }

        UserRequest userRequest = null;

        try {
            userRequest = objectMapper.readValue(requestEvent.getBody(), UserRequest.class);
        } catch (JsonProcessingException exception) {
            lambdaLogger.log("Deserialization exception: %s%n".formatted(exception.getMessage()));
            return buildHttpResponse(emptyList(), "Could not deserialize request body", HttpStatusCode.INTERNAL_SERVER_ERROR, false);
        }

        int numberOfObjects = Math.min(userRequest.numberOfObjects(), 10);
        List<String> objectKeys = new ArrayList<>();

        for (int i = 0; i < numberOfObjects; i++) {
            String requestBody = null;

            try {
                var eventStub = generateEventStub();
                requestBody = objectMapper.writeValueAsString(eventStub);
            } catch (JsonProcessingException exception) {
                lambdaLogger.log("Serialization exception: %s%n".formatted(exception.getMessage()));
                return buildHttpResponse(emptyList(), "Could not serialize request", HttpStatusCode.INTERNAL_SERVER_ERROR, false);
            }

            var objectKey = UUID.randomUUID() + ".json";

            try {
                var putObjectRequest = PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(objectKey)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromString(requestBody));
                objectKeys.add(objectKey);
            } catch (S3Exception exception) {
                lambdaLogger.log("Exception uploading objects: %s%n".formatted(exception));
                return buildHttpResponse(objectKeys, "Could not upload objects", HttpStatusCode.SERVICE_UNAVAILABLE, true);
            }
        }

        return buildHttpResponse(objectKeys, "Objects uploaded to bucket", HttpStatusCode.CREATED, false);
    }

    private APIGatewayV2HTTPResponse buildHttpResponse(List<String> objectKeys, String status, int statusCode, boolean partialKeys) {
        var uploadResponse = new UploadResponse(objectKeys, status, partialKeys);

        String responseBody = null;

        try {
            responseBody = objectMapper.writeValueAsString(uploadResponse);
        } catch (JsonProcessingException exception) {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
                    .withBody("Could not format response")
                    .build();
        }

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(responseBody)
                .build();
    }

    private Event generateEventStub() {
        var userId = UUID.randomUUID().toString();
        var firstName = faker.name().firstName();
        var middleName = faker.name().firstName();
        var lastName = faker.name().lastName();
        var eventType = EventType.randomEventType();
        var createdAt = LocalDateTime.ofInstant(faker.date().past(30, TimeUnit.DAYS).toInstant(), ZoneId.systemDefault());
        var message = faker.rickAndMorty().quote();
        return new Event(userId, firstName, middleName, lastName, eventType, createdAt, message);
    }

}
