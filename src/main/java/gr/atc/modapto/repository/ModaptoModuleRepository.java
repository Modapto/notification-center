package gr.atc.modapto.repository;

import gr.atc.modapto.model.ModaptoModule;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface ModaptoModuleRepository extends ElasticsearchRepository<ModaptoModule, String> {

    Optional<ModaptoModule> findByModuleId(String moduleId);

}
