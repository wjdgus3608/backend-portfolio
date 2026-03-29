package jo.jung.domain.api;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockTradeRankInfoReqDTO implements Serializable {
    private String FID_BLNG_CLS_CODE;
    private String FID_INPUT_PRICE_1;
    private String FID_INPUT_PRICE_2;
    private String FID_VOL_CNT;
    private String FID_INPUT_DATE_1;

    private String FID_RSFL_RATE2;
    private String FID_COND_MRKT_DIV_CODE;
    private String FID_COND_SCR_DIV_CODE;
    private String FID_INPUT_ISCD;
    private String FID_RANK_SORT_CLS_CODE;
    private String FID_INPUT_CNT_1;
    private String FID_PRC_CLS_CODE;
    private String FID_TRGT_CLS_CODE;
    private String FID_TRGT_EXLS_CLS_CODE;
    private String FID_DIV_CLS_CODE;
    private String FID_RSFL_RATE1;
}
