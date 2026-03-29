package jo.jung.clientmodule.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class KisBalanceSync {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JSONObject inquireBalance(
            String baseUrl,      // https://openapi.koreainvestment.com:9443
            String appKey,
            String appSecret,
            String accessToken,
            String cano,         // 계좌번호 앞 8자리
            String acntPrdtCd    // 계좌번호 뒤 2자리
    ) throws Exception {

        String apiUrl = baseUrl + "/uapi/domestic-stock/v1/trading/inquire-balance";

        String query = String.format(
                "?CANO=%s&ACNT_PRDT_CD=%s&AFHR_FLPR_YN=N" +
                        "&OFL_YN=&INQR_DVSN=02&UNPR_DVSN=01" +
                        "&FUND_STTL_ICLD_YN=N&FNCG_AMT_AUTO_RDPT_YN=N" +
                        "&PRCS_DVSN=01&CTX_AREA_FK100=&CTX_AREA_NK100=",
                cano, acntPrdtCd
        );

        URL url = new URL(apiUrl + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("authorization", "Bearer " + accessToken);
        conn.setRequestProperty("appKey", appKey);
        conn.setRequestProperty("appSecret", appSecret);
        conn.setRequestProperty("tr_id", "TTTC8434R");  // 실전 기준
        conn.setRequestProperty("custtype", "P");

        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();

        BufferedReader br;
        if (responseCode == 200) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();
        conn.disconnect();

        Map<String, Object> map =
                objectMapper.readValue(response.toString(), new TypeReference<Map<String, Object>>() {});

        return new JSONObject(map);
    }
}
