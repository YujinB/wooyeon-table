package com.wooyeon.table.domain.venue.infrastructure.persistence;

import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import com.wooyeon.table.domain.venue.domain.repository.VenueRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VenueRepositoryImpl implements VenueRepository {

    private final VenueJpaRepository jpaRepository;

    @Override
    public Venue save(Venue venue) {
        return jpaRepository.save(venue);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public Optional<Venue> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Venue> findAll() {
        return jpaRepository.findAllByOrderByRegionAscIdAsc();
    }

    @Override
    public List<Venue> findAllByIdIn(Collection<Long> ids) {
        return jpaRepository.findAllByIdIn(ids);
    }

    @Override
    public List<Venue> findActiveByRegion(Region region) {
        return jpaRepository.findAllByRegionAndActiveTrueOrderByIdAsc(region);
    }
}
