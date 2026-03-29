package com.jung.app.logdb.service;

import com.jung.app.logdb.entity.Log;
import com.jung.app.logdb.repo.LogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogDBService {
    private final LogRepo logRepo;

    public List<Log> readLog(int page, int size){
        return logRepo.findAll(PageRequest.of(page,size)).getContent();
    }

    public List<Log> readLastLogPage(){
        long lastPage = logRepo.count()/100;
        return logRepo.findAll(PageRequest.of((int)lastPage,100)).getContent();
    }

    public long getTotalCnt(){
        return logRepo.count();
    }

    public void insertLog(String msg){
        Log log = Log.builder().log(msg).build();
        logRepo.save(log);
    }

    public void deleteAllLog(){
        logRepo.deleteAll();
    }
}
