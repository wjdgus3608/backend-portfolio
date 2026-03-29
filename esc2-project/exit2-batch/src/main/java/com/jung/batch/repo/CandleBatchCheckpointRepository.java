package com.jung.batch.repo;

import com.jung.batch.domain.entity.CandleBatchCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandleBatchCheckpointRepository extends JpaRepository<CandleBatchCheckpoint, String> {
}
