package jo.jung.domain.nowprice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NowPrice {
    private long nowPrice;
    private long bottomPrice;
    private long topPrice;
    private long startPrice;

    public static NowPrice jsonToNowPrice(LinkedHashMap json){
        return NowPrice.builder()
                .nowPrice(Long.parseLong((String) json.get("stck_prpr")))
                .bottomPrice(Long.parseLong((String) json.get("stck_lwpr")))
                .topPrice(Long.parseLong((String) json.get("stck_hgpr")))
                .startPrice(Long.parseLong((String) json.get("stck_oprc")))
                .build();
    }
}
