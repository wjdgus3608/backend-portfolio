package jo.jung.backteston.repo;

import jo.jung.backteston.entity.TradeAmountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeAmountRepo extends JpaRepository<TradeAmountEntity,String> {
    @Query(value = """
    SELECT *
    FROM (
        SELECT 
            *,
            ROW_NUMBER() OVER (PARTITION BY time_at ORDER BY trade_amount DESC) AS rn
        FROM trade_amount
    ) ranked
    WHERE rn <= :limitPerDay
    AND time_at < (SELECT MAX(time_at) FROM trade_amount)
    ORDER BY time_at, trade_amount DESC
    """, nativeQuery = true)
    List<TradeAmountEntity> findTopNByTimeAt(@Param("limitPerDay") int limitPerDay);

    // 모든 time_at 날짜를 오름차순 리스트로 반환
    @Query(value = """
        SELECT DISTINCT time_at
        FROM trade_amount
        ORDER BY time_at ASC
        """, nativeQuery = true)
    List<String> getSavedDateList();
}
