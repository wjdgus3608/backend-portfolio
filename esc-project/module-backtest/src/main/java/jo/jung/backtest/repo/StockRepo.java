package jo.jung.backtest.repo;

import jo.jung.backtest.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepo extends JpaRepository<StockEntity,String> {
}
