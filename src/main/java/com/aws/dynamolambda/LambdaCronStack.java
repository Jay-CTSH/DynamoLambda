package com.aws.dynamolambda;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.*;
import software.amazon.awscdk.services.s3.Bucket;

class LambdaCronStack extends Stack {
	public LambdaCronStack(final App parent, final String name, final StackProps props) {
		super(parent, name, props);

        // DynamoDB Table
        Table dynamoDBTable = Table.Builder.create(this, "MyDynamoTable")
                .partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
                .stream(StreamViewType.NEW_IMAGE)
                .build();

        // Lambda Function for CRUD operations
        Function crudLambda = Function.Builder.create(this, "CrudLambdaFunction")
                .runtime(Runtime.JAVA_11)
                .handler("com.example.CrudLambdaHandler::handleRequest") // Example handler class
                .code(Code.fromBucket(Bucket.fromBucketName(this, "CrudLambdaHandlerBucket", "deployment-bucket"), "LambdaHandler/deploy/CrudLambdaHandler.jar"))
                .environment(java.util.Map.of(
                        "MyDynamoTable", dynamoDBTable.getTableName()
                ))
                .build();

        dynamoDBTable.grantReadWriteData(crudLambda);

        Function streamProcessorLambda = Function.Builder.create(this, "StreamProcessorLambdaFunction")
                .runtime(Runtime.JAVA_11)
                .handler("com.example.StreamProcessorLambdaHandler::handleRequest") // Example handler class
                .code(Code.fromBucket(Bucket.fromBucketName(this, "StreamProcessorLambdaHandlerBucket", "deployment-bucket"), "LambdaHandler/deploy/StreamProcessorLambdaHandler.jar"))
                .build();

        dynamoDBTable.grantStreamRead(streamProcessorLambda);

        streamProcessorLambda.addEventSource(new DynamoEventSource(dynamoDBTable, DynamoEventSourceProps.builder()
                .startingPosition(StartingPosition.LATEST)
                .batchSize(10)
                .build()));

        // API Gateway
        RestApi api = RestApi.Builder.create(this, "CrudAPI")
                .restApiName("CrudAPI")
                .build();

        LambdaIntegration crudIntegration = new LambdaIntegration(crudLambda);
        Resource crudResource1 = api.getRoot().addResource("crudFunctions");
        Resource crudResource2 = crudResource1.addResource("{id}");
        
        crudResource1.addMethod("POST", crudIntegration);// Create an item
        
        crudResource2.addMethod("GET", crudIntegration); // Get an item by id
        crudResource2.addMethod("PUT", crudIntegration); // Update an item by id
        crudResource2.addMethod("DELETE", crudIntegration); // Delete an item by id

    }
}