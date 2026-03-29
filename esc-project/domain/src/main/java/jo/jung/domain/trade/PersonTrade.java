package jo.jung.domain.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonTrade {
    private String stockCode;
    private String stockName;
    private long personBuyAmount;
    private long foreignBuyAmount;
    private long agencyBuyAmount;
    private String timeAt;

    public static PersonTrade jsonToPersonTrade(LinkedHashMap json){
        String pAmount = (String) json.get("prsn_ntby_qty");
        String fAmount = (String) json.get("frgn_ntby_qty");
        String aAmount = (String) json.get("orgn_ntby_qty");
        if(pAmount.equals("") && fAmount.equals("") && aAmount.equals("")) return null;
        return PersonTrade.builder()
                .personBuyAmount(Long.parseLong(pAmount))
                .foreignBuyAmount(Long.parseLong(fAmount))
                .agencyBuyAmount(Long.parseLong(aAmount))
                .timeAt((String) json.get("stck_bsop_date"))
                .build();
    }

}
