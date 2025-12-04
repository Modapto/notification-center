package gr.atc.modapto.service;

import gr.atc.modapto.model.ModaptoModule;
import gr.atc.modapto.repository.ModaptoModuleRepository;
import org.springframework.stereotype.Service;

@Service
public class ModaptoModuleService {

    private final ModaptoModuleRepository modaptoModuleRepository;

    public ModaptoModuleService(ModaptoModuleRepository modaptoModuleRepository){
        this.modaptoModuleRepository = modaptoModuleRepository;
    }

    /**
     * Retrieve module name from PKB
     *
     * @param moduleId : Module ID
     * @return String
     */
    public String retrieveModaptoModuleName(String moduleId){
        return modaptoModuleRepository.findByModuleId(moduleId).map(ModaptoModule::getName).orElse(null);
    }
}
