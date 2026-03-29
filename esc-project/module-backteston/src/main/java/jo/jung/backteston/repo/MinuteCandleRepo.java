package jo.jung.backteston.repo;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.backteston.entity.TradeAmountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MinuteCandleRepo extends JpaRepository<MinuteCandleEntity,String> {
    @Query("SELECT m FROM MinuteCandleEntity m " +
            "WHERE m.stockCode = :stockCode " +
            "AND m.timeAt LIKE CONCAT(:datePrefix, '%') " +
            "ORDER BY m.timeAt ASC")
    List<MinuteCandleEntity> findByStockCodeAndDate(@Param("stockCode") String stockCode,
                                                    @Param("datePrefix") String datePrefix);
}
