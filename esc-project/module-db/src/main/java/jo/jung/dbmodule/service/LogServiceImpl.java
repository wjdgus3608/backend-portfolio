package jo.jung.dbmodule.service;

import jo.jung.dbmodule.entity.LogEntity;
import jo.jung.dbmodule.repo.LogRepo;
import jo.jung.domain.log.LogSelectReqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Service
public class LogServiceImpl implements LogService{

    private final LogRepo logRepo;

    @Override
    public Mono<LogEntity> saveLog(LogEntity entity) {
        return logRepo.save(entity);
    }

    @Override
    public Flux<LogEntity> getLog(LogSelectReqDTO input) {
        return logRepo.retrieveLogs(input.getPage(), input.getSize(), input.getType());
    }
}
