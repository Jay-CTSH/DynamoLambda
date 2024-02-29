package com.aws.dynamolambda;

import software.amazon.awscdk.App;
import software.amazon.awscdk.DefaultStackSynthesizer;
import software.amazon.awscdk.StackProps;

public class LambdaCronApp {
	public static void main(final String[] args) {

		App app = new App();

		DefaultStackSynthesizer synthesizer = DefaultStackSynthesizer.Builder.create()
				.generateBootstrapVersionRule(false).build();
		new LambdaCronStack(app, "cdk-lambda-cron-example",
				StackProps.builder().stackName("cdk-lambda-cron-example").synthesizer(synthesizer).build());

		app.synth();

	}
}