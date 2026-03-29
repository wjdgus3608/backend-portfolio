package jo.jung.dbmodule.service;

import jo.jung.dbmodule.entity.LogEntity;
import jo.jung.domain.log.LogSelectReqDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LogService {
    Mono<LogEntity> saveLog(LogEntity entity);
    Flux<LogEntity> getLog(LogSelectReqDTO input);
}
