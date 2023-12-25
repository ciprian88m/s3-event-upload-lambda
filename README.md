# S3 Event Upload Lambda

## About this repo

Lambda handler meant to generate simple stub events and upload them in an S3 bucket. 

A minimum of 1 and maximum of 10 events are generated for each invocation. [Faker](https://github.com/DiUS/java-faker) is used for generating random data.

The events are meant to be used for data analysis.

Sample AWS Athena table setup:

```
create external table events (
    userId string,
    firstName string,
    middleName string,
    lastName string,
    eventType string,
    createdAt timestamp,
    message string
)
row format serde 'org.openx.data.jsonserde.JsonSerDe'
with serdeproperties ('ignore.malformed.json' = 'true')
location 's3://<BUCKET_NAME>/'
```
## Building

The fat jar can be built using:

```commandline
./gradlew clean shadowJar
```
## Infrastructure

The AWS infrastructure repo can be found here: [s3-event-upload-lambda-cdk](https://github.com/ciprian88m/s3-event-upload-lambda-cdk)
