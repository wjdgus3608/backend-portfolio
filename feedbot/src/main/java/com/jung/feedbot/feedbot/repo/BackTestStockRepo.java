package com.jung.feedbot.feedbot.repo;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackTestStockRepo extends JpaRepository<BackTestStock, Long> {
}
