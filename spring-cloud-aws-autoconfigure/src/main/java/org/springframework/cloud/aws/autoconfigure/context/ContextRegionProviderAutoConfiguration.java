/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.autoconfigure.context;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import static org.springframework.cloud.aws.context.config.support.ContextConfigurationUtils.registerRegionProvider;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextRegionProviderAutoConfiguration.Registrar.class)
public class ContextRegionProviderAutoConfiguration {

	static class Registrar implements EnvironmentAware, ImportBeanDefinitionRegistrar {

		private Environment environment;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
			if (this.environment.containsProperty("cloud.aws.region.auto") || this.environment.containsProperty("cloud.aws.region.static")) {
				registerRegionProvider(registry, this.environment.containsProperty("cloud.aws.region.auto"),
						this.environment.getProperty("cloud.aws.region.static"));
			}
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}
	}
}