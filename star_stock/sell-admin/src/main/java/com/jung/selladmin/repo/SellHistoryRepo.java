package com.jung.selladmin.repo;

import com.jung.selladmin.vo.SellHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellHistoryRepo extends JpaRepository<SellHistory,Long> {
}
