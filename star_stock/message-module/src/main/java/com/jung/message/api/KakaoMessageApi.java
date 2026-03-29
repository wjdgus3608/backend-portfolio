package com.jung.message.api;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

public class KakaoMessageApi implements MessageApi{

    @Value("${restApiAppKey}")
    private String apiAppKey;
    @Value("${code}")
    private String code;

    private String accessKey;

    @Override
    public boolean healthCheck() {
        return true;
    }

    @Override
    public ResponseEntity<?> sendMessage(String msg, String linkUrl) {

        getAuthKey("http://127.0.0.1:8081");

        String baseUrl = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                    headers.set("Authorization","Bearer "+accessKey);
                })
                .build();

        System.out.println("@@ apiAppKey : "+accessKey);

        JSONObject jsonObject = generateMessageJson(msg,linkUrl);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("template_object", jsonObject.toString());

        JSONObject response = client.post()
                                        .bodyValue(body)
                                        .retrieve()
                                        .bodyToMono(JSONObject.class)
                                        .block();

        return ResponseEntity.ok().build();
    }

    private JSONObject generateMessageJson(String msg, String linkUrl){
        JSONObject json = new JSONObject();
        JSONObject linkJson = new JSONObject();
        linkJson.put("web_url",linkUrl);
        linkJson.put("mobile_web_url",linkUrl);

        json.put("object_type","text");
        json.put("text",msg);
        json.put("link",linkJson);
        json.put("button_title","자세히 보기");

        return json;
    }

    private String getAuthKey(String redirect_uri){
        String baseUrl = "https://kauth.kakao.com/oauth";

        WebClient client2 = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                })
                .build();



        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type","authorization_code");
        body.add("client_id",apiAppKey);
        body.add("redirect_uri",redirect_uri);
        body.add("code",code);

        System.out.println("@@ code : "+code);
        System.out.println("@@ redirect_uri : "+redirect_uri);
        System.out.println("@@ client_id : "+apiAppKey);

        JSONObject response = client2.post()
                .uri(uriBuilder -> uriBuilder.path("/token")
                .queryParam("grant_type","authorization_code")
                .queryParam("client_id",apiAppKey)
                .queryParam("redirect_uri",redirect_uri)
                .queryParam("code",code)
                .build())
                .retrieve()
                .bodyToMono(JSONObject.class)
                .block();

        accessKey = (String) response.get("access_token");

        return accessKey;
    }

    private JSONObject generateAuthJson(String client_id, String redirect_uri, String code){
        JSONObject jsonObject = new JSONObject();


        return jsonObject;
    }


}
