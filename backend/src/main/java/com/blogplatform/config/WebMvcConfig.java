package com.blogplatform.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves the uploaded images straight from the storage directory. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final StorageProperties storageProperties;

	public WebMvcConfig(StorageProperties storageProperties) {
		this.storageProperties = storageProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path root = Paths.get(storageProperties.location()).toAbsolutePath().normalize();
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(root.toUri().toString())
				.setCachePeriod(3600);
	}
}
