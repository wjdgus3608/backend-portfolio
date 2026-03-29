package com.jung.batch.service.collectcandle;

import com.jung.batch.domain.entity.CandleBatchCheckpoint;
import com.jung.batch.repo.CandleBatchCheckpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandleCheckpointService {

    private final CandleBatchCheckpointRepository checkpointRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCheckpoint(String ticker, String fromTime) {
        checkpointRepository.save(
                new CandleBatchCheckpoint(ticker, fromTime)
        );
    }
}

