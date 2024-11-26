package io.gleecy.foi.tool;

import io.gleecy.foi.util.DTOBase;
import org.moqui.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseClient<T extends DTOBase, R extends DTOBase> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseClient.class);
    public final Map<String, String> requestHeaders = new HashMap<>();
    public final RestClient.RequestFactory requestFactory;
    public final RestClient.Method method;
    public final String token;
    protected String url;
    protected int timeout = 50; //request timeout in seconds
    protected T requestData = null;
    protected R responseData = null;
    protected int statusCode = 0;

    public BaseClient(RestClient.RequestFactory requestFactory, RestClient.Method method, String baseUrl, String token) {
        this.requestFactory = requestFactory;
        this.url = baseUrl;
        this.method = method;
        this.token = token;
    }
    public int statusCode() { return statusCode; }
    protected abstract R newResponse(RestClient.RestResponse response);
    public BaseClient<T, R> withHeader(Http.Header header, String value) {
        requestHeaders.put(header.value, value);
        return this;
    }
    public BaseClient<T, R> withRequestTimeout(int timeout) { this.timeout = timeout; return this;}
    public BaseClient<T, R> withLanguage(String language) { return withHeader(Http.Header.LANGUAGE, language);}
    public BaseClient<T, R> withContentType(Http.ContentType contentType) { return withHeader(Http.Header.CONTENT_TYPE, contentType.value);}
    public BaseClient<T, R> appendPath(String path) { this.url = this.url + path; return this;}
    public BaseClient<T, R> appendPathParams(String pathPattern, Object... params) {
        this.url = this.url + String.format(pathPattern, params);
        return this;
    }
    public BaseClient<T, R> withHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders.putAll(requestHeaders);
        return this;
    }
    public BaseClient<T, R> withRequestData(String key, Object value) {
        this.requestData.put(key, value);
        return this;
    }
    public R getResponseData() { return this.responseData; }
    public String encodeBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
    public String encodeJwt(String... params) {
        StringBuilder jwt = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            jwt.append(encodeBase64(params[i])).append(".");
        }
        return jwt.toString();
    }
    public void call() {
        final RestClient rest = new RestClient()
                .withRequestFactory(requestFactory)
                .method(method).uri(url).timeout(timeout);
        if(token != null) {
            rest.addHeader("Authorization", token);
        }
        if(!requestHeaders.isEmpty()) {
            requestHeaders.forEach(rest::addHeader);
        }
        if(requestData != null) {
            String sContentType = requestHeaders.get(Http.Header.CONTENT_TYPE.value);
            LOGGER.debug("contentType = " + sContentType + ", JSON content type: " + Http.ContentType.JSON.value);
            Http.ContentType contentType = Http.ContentType.fromString(sContentType);
            if(contentType == Http.ContentType.JSON) {
                rest.jsonObject(requestData);
            } else{
                ((Map<?, ?>) requestData).forEach((k,v) -> {
                    LOGGER.debug("k=" + k + ", v=" + v.toString());
                    rest.addBodyParameter((String) k, (String) v);
                });
            }
        }

        LOGGER.info("Requesting " + rest.getUriString() + ", token: " + token + ", body: " + rest.getBodyText() );
        RestClient.RestResponse restResponse = rest.call();
        this.statusCode = restResponse.getStatusCode();
        this.responseData = newResponse(restResponse);
    }
}
