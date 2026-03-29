package jo.jung.domain.candle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;

import java.util.LinkedHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle {
    private CandleType candleType;
    private CandleSign candleSign;

    private long topPrice;
    private long bottomPrice;
    private long startPrice;
    private long endPrice;
    private long tradeAmount;
    private long tradeMoney;
    private String timeAt;

    public static Candle minuteJsonToCandle(LinkedHashMap json){
        return Candle.builder()
                .candleType(CandleType.MINUTE)
                .topPrice(Long.parseLong((String) json.get("stck_hgpr")))
                .bottomPrice(Long.parseLong((String) json.get("stck_lwpr")))
                .startPrice(Long.parseLong((String) json.get("stck_oprc")))
                .endPrice(Long.parseLong((String) json.get("stck_prpr")))
                .tradeAmount(Long.parseLong((String) json.get("cntg_vol")))
                .tradeMoney(Long.parseLong((String) json.get("acml_tr_pbmn")))
                .timeAt((String)json.get("stck_bsop_date")+(String)json.get("stck_cntg_hour"))
                .build();
    }

    public static Candle dailyCandleJsonToCandle(LinkedHashMap json){
        return Candle.builder()
                .candleType(CandleType.MINUTE)
                .topPrice(Long.parseLong((String) json.get("stck_hgpr")))
                .bottomPrice(Long.parseLong((String) json.get("stck_lwpr")))
                .startPrice(Long.parseLong((String) json.get("stck_oprc")))
                .endPrice(Long.parseLong((String) json.get("stck_clpr")))
                .tradeAmount(Long.parseLong((String) json.get("acml_vol")))
                .tradeMoney(Long.parseLong((String) json.get("acml_tr_pbmn")))
                .timeAt((String)json.get("stck_bsop_date"))
                .build();
    }
}
