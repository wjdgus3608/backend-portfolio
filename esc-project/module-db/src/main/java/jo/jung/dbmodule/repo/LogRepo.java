package jo.jung.dbmodule.repo;

import jo.jung.dbmodule.entity.LogEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface LogRepo extends ReactiveCrudRepository<LogEntity, Long>, LogRepositoryCustom{
}
