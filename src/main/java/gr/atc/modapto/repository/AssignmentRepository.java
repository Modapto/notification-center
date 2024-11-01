package gr.atc.modapto.repository;

import gr.atc.modapto.model.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AssignmentRepository extends ElasticsearchRepository<Assignment, String> {
    Page<Assignment> findBySourceUserId(String userId, Pageable pageable);

    Page<Assignment> findBySourceUserIdAndStatus(String userId, String status, Pageable pageable);

    Page<Assignment> findByTargetUserId(String target, Pageable pageable);

    Page<Assignment> findByTargetUserIdAndStatus(String target, String status, Pageable pageable);

    Page<Assignment> findBySourceUserIdOrTargetUserId (String sourceUserId, String targetUserId, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"status\": \"?0\"}}], \"should\": [{\"match\": {\"sourceUserId\": \"?1\"}}, {\"match\": {\"targetUserId\": \"?2\"}}], \"minimum_should_match\": 1}}")
    Page<Assignment> findByStatusAndSourceUserIdOrTargetUserId(String status, String sourceUserId, String targetUserId, Pageable pageable);
}
