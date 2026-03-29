package jo.jung.backtest.repo;

import jo.jung.backtest.entity.MinuteCandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinuteCandleRepo extends JpaRepository<MinuteCandleEntity,String> {
}
