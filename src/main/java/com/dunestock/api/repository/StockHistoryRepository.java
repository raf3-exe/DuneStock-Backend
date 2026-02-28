package com.dunestock.api.repository;

import com.dunestock.api.model.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, String> {
    List<StockHistory> findByUserUserId(String userId);
}