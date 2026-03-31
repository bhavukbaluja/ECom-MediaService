package com.eCom.MediaService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"com.eCom.MediaService", "com.eCom.Commons"})
@EnableJpaRepositories(basePackages = {"com.eCom.MediaService.repository", "com.eCom.Commons.repository"})
// CRITICAL: Scans for @Entity classes (The actual database tables) inside Commons
@EntityScan(basePackages = {"com.eCom.MediaService.model", "com.eCom.Commons.model"})
@EnableJpaAuditing
@EnableAsync
public class MediaServiceApplication implements CommandLineRunner{
	private final ApplicationContext context;

	public MediaServiceApplication(ApplicationContext context) {
		this.context = context;
	}

	public static void main(String[] args) {
		SpringApplication.run(MediaServiceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("🔍 Beans Loaded:");
		for (String beanName : context.getBeanDefinitionNames()) {
			if (beanName.contains("companyController")) {
				System.out.println("✅ FOUND: " + beanName);
			}
		}
	}
}