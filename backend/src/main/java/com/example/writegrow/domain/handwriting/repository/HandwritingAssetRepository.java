package com.example.writegrow.domain.handwriting.repository;

import com.example.writegrow.domain.handwriting.entity.HandwritingAsset;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandwritingAssetRepository extends JpaRepository<HandwritingAsset, Long> {

    Optional<HandwritingAsset> findByWritingId(Long writingId);
}
