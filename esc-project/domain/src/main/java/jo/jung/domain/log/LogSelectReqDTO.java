package jo.jung.domain.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogSelectReqDTO {
    private int page;
    private int size;
    private String type;
}
