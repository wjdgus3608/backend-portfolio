package com.jung.selladmin.service;

import com.jung.selladmin.dto.RetrieveStockRtnDTO;
import com.jung.selladmin.dto.SaveStockStatusReqDTO;

import java.util.List;

public interface AutoTradeService {
    void runAutoTrade();
    List<RetrieveStockRtnDTO> retrieveAccount();
    void saveAccountStatus(List<SaveStockStatusReqDTO> input);
}
