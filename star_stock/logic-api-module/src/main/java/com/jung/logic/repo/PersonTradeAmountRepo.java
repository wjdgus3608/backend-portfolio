package com.jung.logic.repo;

import com.jung.logic.vo.PersonTradeAmountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonTradeAmountRepo extends JpaRepository<PersonTradeAmountEntity, String> {
    List<PersonTradeAmountEntity> findByStockCodeOrderByTimeAtAsc(String stockCode);
}
