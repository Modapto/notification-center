package gr.atc.modapto;

import gr.atc.modapto.repository.AssignmentRepository;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;
import gr.atc.modapto.repository.NotificationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles(profiles = "test")
class ModaptoNotificationCenterApplicationTests {

	@MockitoBean
	private ElasticsearchOperations elasticsearchOperations;

	@MockitoBean
	private ElasticsearchTemplate elasticsearchTemplate;

	@MockitoBean
	private NotificationRepository notificationRepository;

	@MockitoBean
	private AssignmentRepository assignmentRepository;

	@MockitoBean
	private EventRepository eventRepository;

	@MockitoBean
	private EventMappingsRepository eventMappingsRepository;

	@Test
	void contextLoads() {
		Assertions.assertNotNull(ApplicationContext.class);
	}

}
