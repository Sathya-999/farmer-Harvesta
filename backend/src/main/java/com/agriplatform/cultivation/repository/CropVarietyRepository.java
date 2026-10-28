package com.agriplatform.cultivation.repository;

import com.agriplatform.cultivation.entity.CropVariety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CropVarietyRepository extends JpaRepository<CropVariety, UUID> {

    List<CropVariety> findAllByCropIdAndIsActiveTrueOrderByNameAsc(UUID cropId);

    Optional<CropVariety> findByIdAndCropId(UUID id, UUID cropId);
}
