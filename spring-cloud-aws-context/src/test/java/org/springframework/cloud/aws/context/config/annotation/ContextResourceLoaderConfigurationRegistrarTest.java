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

package org.springframework.cloud.aws.context.config.annotation;


import org.junit.After;
import org.junit.Test;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertNotNull;

public class ContextResourceLoaderConfigurationRegistrarTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void regionProvider_withConfiguredRegion_staticRegionProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithResourceLoader.class);

		//Act
		PathMatchingSimpleStorageResourcePatternResolver resourceLoader =
				this.context.getBean(PathMatchingSimpleStorageResourcePatternResolver.class);

		//Assert
		assertNotNull(resourceLoader);
	}

	@EnableContextResourceLoader
	static class ApplicationConfigurationWithResourceLoader {

	}
}