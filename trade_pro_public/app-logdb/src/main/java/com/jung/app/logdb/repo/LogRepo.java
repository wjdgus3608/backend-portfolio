package com.jung.app.logdb.repo;

import com.jung.app.logdb.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepo extends JpaRepository<Log,Long> {
}
