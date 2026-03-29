package jo.jung.dbmodule.repo;

import jo.jung.dbmodule.entity.LogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;


@Repository
@RequiredArgsConstructor
public class LogRepositoryCustomImpl implements LogRepositoryCustom {

    private final DatabaseClient client;

    @Override
    public Flux<LogEntity> retrieveLogs(int page, int size, String type) {
        int offset = page * size;

        String query = "SELECT * FROM logs where type = :type ORDER BY created_at LIMIT :size OFFSET :offset";

        return client.sql(query)
                .bind("size", size)
                .bind("offset", offset)
                .bind("type", type)
                .map((row, metadata) -> LogEntity.builder()
                        .id(row.get("id", Long.class))
                        .message(row.get("message", String.class))
                        .createdAt(row.get("created_at", LocalDateTime.class))
                        .build())
                .all();
    }
}
