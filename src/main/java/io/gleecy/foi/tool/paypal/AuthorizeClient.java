package io.gleecy.foi.tool.paypal;

import io.gleecy.foi.paypal.UnitAmount;
import io.gleecy.foi.util.DTOBase;
import io.gleecy.foi.paypal.PayPalResponse;
import io.gleecy.foi.paypal.PayerBase;
import io.gleecy.foi.paypal.PaymentSource;
import io.gleecy.foi.tool.Http;
import org.moqui.util.RestClient;

import java.util.Map;

public class AuthorizeClient extends PayPalClient<DTOBase, PayPalResponse> {
    public AuthorizeClient(RestClient.RequestFactory requestFactory, String baseUrl, String token) {
        super(requestFactory, baseUrl, token);
        this.requestData = new DTOBase();
    }

    @Override
    protected PayPalResponse newResponse(RestClient.RestResponse response) {
        return new PayPalResponse(response);
    }
    public void setAuthAssertionJwt(String paypalAccountId) {
        withHeader(Http.Header.AUTH_ASSERT, createAuthAssertionJwt(paypalAccountId));
    }
    public void setPaymentSource(PayerBase.Type type) {
        requestData.put("payment_source", new PaymentSource(type));
    }
    public void setPaymentSource(Map<String, Object> data) {
        PaymentSource paymentSource = new PaymentSource(data);
        requestData.put("payment_source", paymentSource);
    }

    public void setAmount(double amount, String currencyCode) {
        requestData.put("amount", new UnitAmount("" + amount, currencyCode));
    }
}
