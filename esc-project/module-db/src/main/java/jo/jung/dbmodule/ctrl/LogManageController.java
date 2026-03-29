package jo.jung.dbmodule.ctrl;

import jo.jung.dbmodule.entity.LogEntity;
import jo.jung.dbmodule.service.LogService;
import jo.jung.domain.log.LogInsertReqDTO;
import jo.jung.domain.log.LogSelectReqDTO;
import jo.jung.domain.log.LogSelectResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LogManageController {

    private final LogService logService;

    @GetMapping("/log")
    public Flux<LogSelectResDTO> getLogs(@RequestParam int page,  @RequestParam int size, @RequestParam String type){
        LogSelectReqDTO reqDTO = LogSelectReqDTO.builder()
                .page(page)
                .size(size)
                .type(type)
                .build();

        return logService.getLog(reqDTO).doOnNext(logEntity -> {
            log.info("LogEntity: id=" + logEntity.getId() + ", message=" + logEntity.getMessage() + ", createdAt=" + logEntity.getCreatedAt());
        }).map(entity -> LogSelectResDTO.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .createAt(entity.getCreatedAt())
                .build());
    }

    @PostMapping("/log")
    public Mono<LogSelectResDTO> saveLog(@RequestBody LogInsertReqDTO reqDTO){

        log.info(reqDTO.toString());

        LogEntity entity = LogEntity.builder()
                .type(reqDTO.getType())
                .message(reqDTO.getMessage())
                .build();

        log.info(entity.toString());

        return logService.saveLog(entity) // Mono<LogEntity>
                .map(saved -> LogSelectResDTO.builder()
                        .id(saved.getId())
                        .message(saved.getMessage())
                        .createAt(saved.getCreatedAt())
                        .build());
    }
}
