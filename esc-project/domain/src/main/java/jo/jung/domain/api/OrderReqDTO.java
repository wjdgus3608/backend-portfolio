package jo.jung.domain.api;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderReqDTO implements Serializable {
    private String CANO;
    private String ACNT_PRDT_CD;
    private String PDNO;
    private String SLL_TYPE;
    private String ORD_DVSN;
    private String ORD_QTY;
    private String ORD_UNPR;
    private String CNDT_PRIC;
    private String EXCG_ID_DVSN_CD;
}
