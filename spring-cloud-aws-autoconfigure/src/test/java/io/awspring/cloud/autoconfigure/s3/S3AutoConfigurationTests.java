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
package io.awspring.cloud.autoconfigure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.s3.DiskBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3OutputStream;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3Template;
import io.awspring.cloud.s3.crossregion.CrossRegionS3Client;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Tests for {@link S3AutoConfiguration}.
 *
 * @author Maciej Walkowiak
 */
class S3AutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3AutoConfiguration.class));

	@Test
	void createsS3ClientBean() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(S3Client.class);
			S3Client s3Client = context.getBean(S3Client.class);
			assertThat(s3Client).isInstanceOf(CrossRegionS3Client.class);

			assertThat(context).hasSingleBean(S3ClientBuilder.class);
			assertThat(context).hasSingleBean(S3Properties.class);
			assertThat(context).hasSingleBean(S3OutputStreamProvider.class);
		});
	}

	@Test
	void s3AutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.s3.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(S3Client.class);
			assertThat(context).doesNotHaveBean(S3ClientBuilder.class);
			assertThat(context).doesNotHaveBean(S3Properties.class);
		});
	}

	@Test
	void autoconfigurationIsNotTriggeredWhenS3ModuleIsNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(S3OutputStreamProvider.class)).run(context -> {
			assertThat(context).doesNotHaveBean(S3Client.class);
			assertThat(context).doesNotHaveBean(S3ClientBuilder.class);
			assertThat(context).doesNotHaveBean(S3Properties.class);
		});
	}

	@Nested
	class S3ClientTests {
		@Test
		void byDefaultCreatesCrossRegionS3Client() {
			contextRunner.run(
					context -> assertThat(context).getBean(S3Client.class).isInstanceOf(CrossRegionS3Client.class));
		}

		@Test
		void s3ClientCanBeOverwritten() {
			contextRunner.withUserConfiguration(CustomS3ClientConfiguration.class).run(context -> {
				assertThat(context).hasSingleBean(S3Client.class);
				assertThat(context).getBean(S3Client.class).isNotInstanceOf(CrossRegionS3Client.class);
			});
		}

		@Test
		void createsStandardClientWhenCrossRegionModuleIsNotInClasspath() {
			contextRunner.withClassLoader(new FilteredClassLoader(CrossRegionS3Client.class)).run(context -> {
				assertThat(context).doesNotHaveBean(CrossRegionS3Client.class);
				assertThat(context).hasSingleBean(S3Client.class);
			});
		}
	}

	@Nested
	class OutputStreamProviderTests {
		@Test
		void byDefaultCreatesDiskBufferingS3OutputStreamProvider() {
			contextRunner.run(context -> assertThat(context).hasSingleBean(DiskBufferingS3OutputStreamProvider.class));
		}

		@Test
		void customS3OutputStreamProviderCanBeConfigured() {
			contextRunner.withUserConfiguration(CustomS3OutputStreamProviderConfiguration.class)
					.run(context -> assertThat(context).hasSingleBean(CustomS3OutputStreamProvider.class));
		}
	}

	@Nested
	class EndpointConfigurationTests {
		@Test
		void withCustomEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.s3.endpoint:http://localhost:8090").run(context -> {
				S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
				ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
				assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(client.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090").run(context -> {
				S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
				ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
				assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(client.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpointAndS3Endpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
					"spring.cloud.aws.s3.endpoint:http://localhost:9999").run(context -> {
						S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
						ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
						assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
						assertThat(client.isEndpointOverridden()).isTrue();
					});
		}
	}

	@Nested
	class S3TemplateAutoConfigurationTests {

		@Test
		void withJacksonOnClasspathAutoconfiguresObjectConverter() {
			contextRunner.run(context -> {
				assertThat(context).hasSingleBean(S3ObjectConverter.class);
				assertThat(context).hasSingleBean(S3Template.class);
			});
		}

		@Test
		void withoutJacksonOnClasspathDoesNotConfigureObjectConverter() {
			contextRunner.withClassLoader(new FilteredClassLoader(ObjectMapper.class)).run(context -> {
				assertThat(context).doesNotHaveBean(S3ObjectConverter.class);
				assertThat(context).doesNotHaveBean(S3Template.class);
			});
		}

		@Test
		void usesCustomObjectMapperBean() {
			contextRunner.withUserConfiguration(CustomJacksonConfiguration.class).run(context -> {
				S3ObjectConverter bean = context.getBean(S3ObjectConverter.class);
				ObjectMapper objectMapper = (ObjectMapper) ReflectionTestUtils.getField(bean, "objectMapper");
				assertThat(objectMapper).isEqualTo(context.getBean("customObjectMapper"));
			});
		}

		@Test
		void usesCustomS3ObjectConverter() {
			contextRunner
					.withUserConfiguration(CustomJacksonConfiguration.class, CustomS3ObjectConverterConfiguration.class)
					.run(context -> {
						S3ObjectConverter s3ObjectConverter = context.getBean(S3ObjectConverter.class);
						S3ObjectConverter customS3ObjectConverter = (S3ObjectConverter) context
								.getBean("customS3ObjectConverter");
						assertThat(s3ObjectConverter).isEqualTo(customS3ObjectConverter);

						S3Template s3Template = context.getBean(S3Template.class);

						S3ObjectConverter converter = (S3ObjectConverter) ReflectionTestUtils.getField(s3Template,
								"s3ObjectConverter");
						assertThat(converter).isEqualTo(customS3ObjectConverter);
					});
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJacksonConfiguration {
		@Bean
		ObjectMapper customObjectMapper() {
			return new ObjectMapper();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3ObjectConverterConfiguration {

		@Bean
		S3ObjectConverter customS3ObjectConverter() {
			return mock(S3ObjectConverter.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3ClientConfiguration {

		@Bean
		S3Client customS3Client() {
			return mock(S3Client.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3OutputStreamProviderConfiguration {

		@Bean
		S3OutputStreamProvider customS3OutputStreamProvider() {
			return new CustomS3OutputStreamProvider();
		}

	}

	static class CustomS3OutputStreamProvider implements S3OutputStreamProvider {

		@Override
		public S3OutputStream create(String bucket, String key, @Nullable ObjectMetadata metadata) throws IOException {
			return null;
		}
	}

}
