package io.gleecy.foi.tool;

import io.gleecy.foi.util.DTOBase;
import org.moqui.util.RestClient;

import java.sql.Timestamp;
import java.util.Map;

public class TokenClient extends BaseClient<DTOBase, DTOBase>{
    public TokenClient(RestClient.RequestFactory requestFactory, RestClient.Method method, String baseUrl, String token) {
        super(requestFactory, method, baseUrl, token);
        requestData = new DTOBase();
    }

    protected String keyBearerToken() { return "access_token"; }
    protected String keyLifeTime() { return "expires_in"; }
    protected String keyExpiryTime() { return "expiry_time"; }
    protected String keyMessage() { return  "message"; }
    public String getBearerToken() {
        return (String) responseData.get(keyBearerToken());
    }
    public Timestamp getExpiryTime() {
        return (Timestamp) responseData.get(keyExpiryTime());
    }
    public String getMessage() { return (String) responseData.get(keyMessage()); }

    public int getLifeTimeInMilliSeconds() {
        Integer lifeTime = (Integer) responseData.get(keyLifeTime());
        if(lifeTime == null) return -1;
        return lifeTime * 1000;
    }

    @Override
    protected DTOBase newResponse(RestClient.RestResponse response) {
        Map<String, Object> responseData = (Map<String, Object>) response.jsonObject();
        Timestamp expiryTime = (Timestamp) responseData.get(keyExpiryTime());
        if(expiryTime == null) {
            Integer lifeTime = (Integer) responseData.get(keyLifeTime());
            if(lifeTime != null) {
                expiryTime = new Timestamp(lifeTime*1000L + System.currentTimeMillis());
                responseData.put(keyExpiryTime(), expiryTime);
            }
        }
        return new DTOBase(responseData);
    }
}
