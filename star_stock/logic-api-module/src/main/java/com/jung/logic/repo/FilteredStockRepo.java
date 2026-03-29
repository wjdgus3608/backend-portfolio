package com.jung.logic.repo;

import com.jung.logic.vo.FilteredStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FilteredStockRepo extends JpaRepository<FilteredStock,Long> {
    @Query("select f.searchTime from FilteredStock f group by f.searchTime")
    List<String> getLogList();
    List<FilteredStock> findBySearchTime(String searchTime);
}
