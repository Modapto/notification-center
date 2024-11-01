package gr.atc.modapto.integration;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class SetupTestContainersEnvironment {
    private static final String ELASTICSEARCH_VERSION = "8.15.0";
    private static final String ELASTIC_SEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION;
    private static final String ELASTIC_SEARCH_USERNAME = "elastic";
    private static final String ELASTIC_SEARCH_PASSWORD = "password";

    protected static ElasticsearchContainer elasticsearchContainer;
    protected static RestClient elasticsearchClient;

    static {
        initializeContainer();
    }

    private static void initializeContainer() {
        try {
            elasticsearchContainer = createAndStartContainer();
            setupElasticsearchClient();
            validateElasticsearchConnection();

            // Register shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(SetupTestContainersEnvironment::cleanupResources));
        } catch (Exception e) {
            cleanupResources();
            throw new RuntimeException("Failed to initialize Elasticsearch container", e);
        }
    }

    private static ElasticsearchContainer createAndStartContainer() {
        ElasticsearchContainer container = new ElasticsearchContainer(
                DockerImageName.parse(ELASTIC_SEARCH_IMAGE)
        )
                .withEnv("xpack.security.enabled", "true")
                .withEnv("xpack.security.transport.ssl.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false")
                .withEnv("xpack.license.self_generated.type", "basic")
                .withPassword(ELASTIC_SEARCH_PASSWORD);

        try {
            container.start();
            return container;
        } catch (Exception e) {
            container.stop();
            throw e;
        }
    }

    private static void setupElasticsearchClient() {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(ELASTIC_SEARCH_USERNAME, ELASTIC_SEARCH_PASSWORD)
        );

        elasticsearchClient = RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress()))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();
    }

    private static void validateElasticsearchConnection() {
        try {
            Response response = elasticsearchClient.performRequest(new Request("GET", "/_cluster/health"));
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                throw new RuntimeException("Failed to validate Elasticsearch connection. Status code: " + statusCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate Elasticsearch connection", e);
        }
    }

    @AfterAll
    static void tearDown() {
        cleanupResources();
    }

    private static void cleanupResources() {
        try {
            if (elasticsearchClient != null) {
                elasticsearchClient.close();
            }

            if (elasticsearchContainer != null && elasticsearchContainer.isRunning()) {
                elasticsearchContainer.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop/close Elasticsearch connection", e);
        }
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
        registry.add("spring.elasticsearch.username", () -> ELASTIC_SEARCH_USERNAME);
        registry.add("spring.elasticsearch.password", () -> ELASTIC_SEARCH_PASSWORD);
    }

    protected static String getElasticsearchUrl() {
        return elasticsearchContainer.getHttpHostAddress();
    }

    protected static RestClient getElasticsearchClient() {
        return elasticsearchClient;
    }
}