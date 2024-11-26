package io.gleecy.foi.tool.paypal;

import io.gleecy.foi.paypal.Operation;
import io.gleecy.foi.tool.ClientConfig;
import io.gleecy.foi.tool.Http;
import io.gleecy.foi.tool.TokenClient;
import org.moqui.context.ExecutionContextFactory;
import org.moqui.util.RestClient;

public class PPClientConfig extends ClientConfig {
    protected PPClientConfig(String configId, ExecutionContextFactory ecf, RestClient.RequestFactory requestFactory) {
        super(configId, ecf, requestFactory);
        keyBaseUrl = "transactionUrl";
        keyExpiryTime = "expiryDate";
    }

    @Override
    public TokenClient newTokenClient() {
        if(error != null) throw new RuntimeException("ClientConfig has unresolved error: " + error);
        TokenClient client = new TokenClient(requestFactory, RestClient.Method.POST, getBaseUrl(), "Basic " + getAuthToken());

        client.withContentType(Http.ContentType.FORM_URL_ENCODED)
                .appendPath("/v1/oauth2/token")
                .withLanguage("en-US")
                .withRequestData("grant_type", "client_credentials");
        return client;
    }

    public OrderCreateClient newOrderCreateClient() {
        if(error != null) throw new RuntimeException("ClientConfig has unresolved error: " + error);
        OrderCreateClient client = new OrderCreateClient(requestFactory, getBaseUrl(), "Bearer " +  getBearerToken());

        client.withContentType(Http.ContentType.JSON)
                .appendPath("/v2/checkout/orders")
                .withLanguage("en-US");
        return client;
    }

    public AuthorizeClient newAuthCaptureClient(String paypalOrderId, Operation op) {
        if(error != null) throw new RuntimeException("ClientConfig has unresolved error: " + error);
        AuthorizeClient client = new AuthorizeClient(requestFactory, getBaseUrl(), "Bearer " +  getBearerToken());

        client.withContentType(Http.ContentType.JSON)
                .appendPath("/v2/checkout/orders/")
                .appendPath(paypalOrderId)
                .appendPath("/").appendPath(op.name().toLowerCase())
                .withLanguage("en-US");
//        if(op == Operation.AUTHORIZE) {
//            client.appendPath("/authorize");
//        } else if (op == Operation.CAPTURE) {
//            client.appendPath("/capture");
//        } else {
//            throw new RuntimeException("Invalid operation for newAuthCaptureClient: " + op.name());
//        }
        return client;
    }

    @Override
    public String getEntityName() {
        return "mantle.account.method.PayPalGatewayConfig";
    }

    public String getAccountId() {
        return (String) this.cfgMap.get("accountId");
    }
}
