package jo.jung.domain.trade;

import jo.jung.domain.candle.Candle;
import jo.jung.domain.candle.CandleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoTrade {
    private long totalBuyRemain;
    private long totalSellRemain;
    private long tradePrice;
    private long sellHo1;
    private long tradeAmount;
    private String timeAt;

    public static HoTrade jsonToHoTrade(LinkedHashMap json){
        return HoTrade.builder()
                .totalBuyRemain(Long.parseLong((String) json.get("total_bidp_rsqn")))
                .totalSellRemain(Long.parseLong((String) json.get("total_askp_rsqn")))
                .sellHo1(Long.parseLong((String) json.get("askp1")))
                .timeAt((String) json.get("aspr_acpt_hour"))
                .build();
    }

    public static HoTrade jsonToHoTradeAmount(LinkedHashMap json){
        return HoTrade.builder()
                .tradeAmount(Long.parseLong((String) json.get("cntg_vol")))
                .tradePrice(Long.parseLong((String) json.get("stck_prpr")))
                .timeAt((String) json.get("stck_cntg_hour"))
                .build();
    }

    public static HoTrade jsonToTimelyTradeAmount(LinkedHashMap json){
        return HoTrade.builder()
                .tradeAmount(Long.parseLong((String) json.get("cnqn")))
                .tradePrice(Long.parseLong((String) json.get("stck_prpr")))
                .timeAt((String) json.get("stck_cntg_hour"))
                .build();
    }
}
