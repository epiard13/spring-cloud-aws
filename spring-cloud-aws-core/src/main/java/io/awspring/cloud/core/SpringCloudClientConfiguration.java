/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.core;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;

/**
 * Utility class for creating {@link ClientOverrideConfiguration} containing "Spring Cloud AWS" user agent. When used,
 * server side (AWS or AWS-compatible service) can measure how many requests to its services come from Spring Cloud AWS.
 *
 * @author Eddú Meléndez
 */
public final class SpringCloudClientConfiguration {

	private static final String NAME = "spring-cloud-aws";

	private static final String VERSION = "3.0";

	private SpringCloudClientConfiguration() {

	}

	public static ClientOverrideConfiguration clientOverrideConfiguration() {
		return ClientOverrideConfiguration.builder()
				.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX, getUserAgent()).build();
	}

	private static String getUserAgent() {
		return NAME + "/" + VERSION;
	}

}
