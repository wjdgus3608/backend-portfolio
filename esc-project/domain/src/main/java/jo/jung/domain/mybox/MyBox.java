package jo.jung.domain.mybox;

import jo.jung.domain.stock.Stock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyBox {
    private String uuid;
    private long money;
    private Map<String ,Stock> stockMap;
    private Set<String> timeSet;
}
