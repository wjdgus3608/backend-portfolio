package com.jung.domain.log;

import com.jung.domain.common.StatusEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class LogDto {
    private StatusEnum status;
    private String message;
    private Object data;

    public static LogDto logListToLogDto(List<LogVO> list){
        return LogDto.builder()
                .status(StatusEnum.OK)
                .message(StatusEnum.OK.message)
                .data(list)
                .build();
    }
}
