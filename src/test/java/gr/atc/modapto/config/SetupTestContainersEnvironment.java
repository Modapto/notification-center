package gr.atc.modapto.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public abstract class SetupTestContainersEnvironment {

    protected static final ElasticsearchContainer elasticsearchContainer = ElasticsearchTestContainer.getInstance();
    protected static final RestClient elasticsearchClient;

    static {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "password")
        );

        elasticsearchClient = RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress()))
                .setHttpClientConfigCallback(builder -> builder.setDefaultCredentialsProvider(credentialsProvider))
                .build();
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
        registry.add("spring.elasticsearch.username", () -> "elastic");
        registry.add("spring.elasticsearch.password", () -> "password");
    }
}
