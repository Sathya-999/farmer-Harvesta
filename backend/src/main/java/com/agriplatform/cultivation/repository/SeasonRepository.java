package com.agriplatform.cultivation.repository;

import com.agriplatform.cultivation.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {

    List<Season> findAllByIsActiveTrueOrderByNameAsc();

    Optional<Season> findByNameIgnoreCase(String name);
}
