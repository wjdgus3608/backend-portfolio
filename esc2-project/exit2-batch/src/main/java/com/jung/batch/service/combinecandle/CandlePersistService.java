package com.jung.batch.service.combinecandle;

import com.jung.batch.domain.entity.CandleAggCheckpoint;
import com.jung.batch.domain.entity.CandleEntity;
import com.jung.batch.repo.CandleAggCheckpointRepository;
import com.jung.batch.repo.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandlePersistService {

    private final CandleRepository candleRepository;
    private final CandleAggCheckpointRepository checkpointRepository;

    @Transactional
    public void saveDataAndCheckpoint(
            CandleEntity entity,
            CandleAggCheckpoint checkpoint
    ) {
        candleRepository.save(entity);
        checkpointRepository.save(checkpoint);
    }
}

