package jo.jung.domain.api;

import jo.jung.domain.stock.Stock;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyBoxReqDTO implements Serializable {
    private Stock stock;
    private String FID_INPUT_HOUR_1;
}
