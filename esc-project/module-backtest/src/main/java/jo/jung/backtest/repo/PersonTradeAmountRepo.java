package jo.jung.backtest.repo;

import jo.jung.backtest.entity.PersonTradeAmountEntity;
import jo.jung.backtest.entity.TradeAmountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonTradeAmountRepo extends JpaRepository<PersonTradeAmountEntity, String> {
}
