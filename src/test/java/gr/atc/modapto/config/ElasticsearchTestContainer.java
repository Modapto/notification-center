package gr.atc.modapto.config;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

public class ElasticsearchTestContainer {
    private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.15.0";

    private static final ElasticsearchContainer container = new ElasticsearchContainer(
            DockerImageName.parse(ELASTICSEARCH_IMAGE))
            .withEnv("xpack.security.enabled", "true")
            .withEnv("xpack.security.transport.ssl.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withEnv("xpack.license.self_generated.type", "basic")
            .withEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")
            .withPassword("password");

    static {
        container.start();
    }

    public static ElasticsearchContainer getInstance() {
        return container;
    }
}
