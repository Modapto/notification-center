package gr.atc.modapto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableElasticsearchRepositories(basePackages = "gr.atc.modapto.repository")
@SpringBootApplication
public class ModaptoNotificationCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModaptoNotificationCenterApplication.class, args);
	}

}
