package jo.jung.dbmodule.repo;

import jo.jung.dbmodule.entity.LogEntity;
import reactor.core.publisher.Flux;

public interface LogRepositoryCustom {
    Flux<LogEntity> retrieveLogs(int page, int size, String type);

}
