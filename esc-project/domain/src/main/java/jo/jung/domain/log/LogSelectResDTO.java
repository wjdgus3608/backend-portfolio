package jo.jung.domain.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogSelectResDTO {
    private long id;
    private String type;
    private String message;
    private LocalDateTime createAt;
}
