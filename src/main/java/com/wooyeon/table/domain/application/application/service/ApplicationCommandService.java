package com.wooyeon.table.domain.application.application.service;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationCommandService {

    private final ApplicationRepository applicationRepository;

    public Long save(Application application) {
        return applicationRepository.save(application).getId();
    }

    public void confirmMatch(Collection<Long> applicationIds, Long slotId) {
        applicationRepository.findAllByIdIn(applicationIds).forEach(application -> application.confirmMatch(slotId));
    }

    public void offerAlternative(Collection<Long> applicationIds) {
        applicationRepository.findAllByIdIn(applicationIds).forEach(Application::offerAlternative);
    }
}
