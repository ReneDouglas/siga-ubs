package br.com.tecsus.sccubs.services;

import br.com.tecsus.sccubs.entities.BasicHealthUnit;
import br.com.tecsus.sccubs.repositories.BasicHealthUnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasicHealthUnitService {

    private final BasicHealthUnitRepository basicHealthUnitRepository;

    public BasicHealthUnitService(BasicHealthUnitRepository basicHealthUnitRepository) {
        this.basicHealthUnitRepository = basicHealthUnitRepository;
    }

    public List<BasicHealthUnit> findBasicHealthUnitsByCityHallOfLoggedSystemUser(String loggedUser) {
        return basicHealthUnitRepository.findByCityHallId(1l);
    }
}
