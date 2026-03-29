package com.jung.logic.service.log;

import com.jung.logic.repo.FilteredStockRepo;
import com.jung.logic.vo.FilteredStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {
    private final FilteredStockRepo filteredStockRepo;

    public void saveLog(FilteredStock filteredStock){
        filteredStockRepo.save(filteredStock);
    }

    public List<String> retrieveLogList(){
        return filteredStockRepo.getLogList();
    }

    public List<FilteredStock> retrieveDetailLog(String searchTime){
        return filteredStockRepo.findBySearchTime(searchTime);
    }
}
