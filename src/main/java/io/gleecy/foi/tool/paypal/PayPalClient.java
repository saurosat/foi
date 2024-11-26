package io.gleecy.foi.tool.paypal;

import io.gleecy.foi.paypal.PayPalResponse;
import io.gleecy.foi.util.DTOBase;
import io.gleecy.foi.tool.BaseClient;
import io.gleecy.foi.tool.Http;
import org.moqui.util.RestClient;

public abstract class PayPalClient<T extends DTOBase, R extends PayPalResponse> extends BaseClient<T, R> {
    public PayPalClient(RestClient.RequestFactory requestFactory, String baseUrl, String token) {
        super(requestFactory, RestClient.Method.POST, baseUrl, token);
        this.withContentType(Http.ContentType.JSON);
    }
    public String createAuthAssertionJwt(String paypalAccountId) {
        String header = "{\"alg\":\"none\"}";
        String payload = "{\"payer_id\":\"" + paypalAccountId + "\"}";
        return encodeJwt(header, payload);
    }
    public void setRequestId(String requestId) {
        this.withHeader(Http.Header.PAYPAL_REQ_ID, requestId);
    }

}
