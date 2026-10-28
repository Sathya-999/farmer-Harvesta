package com.agriplatform.cultivation.repository;

import com.agriplatform.cultivation.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CropRepository extends JpaRepository<Crop, UUID> {

    List<Crop> findAllByIsActiveTrueOrderByNameAsc();

    Optional<Crop> findByNameIgnoreCase(String name);
}
