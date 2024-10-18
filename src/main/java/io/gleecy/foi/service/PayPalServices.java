package io.gleecy.foi.service;

import groovy.json.JsonOutput;
import org.eclipse.jetty.http.HttpHeader;
import org.moqui.context.ExecutionContext;
import org.moqui.entity.EntityFacade;
import org.moqui.entity.EntityValue;
import org.moqui.util.ContextStack;
import org.moqui.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class PayPalServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayPalServices.class);
    public static final Map<String, String> OPT_MAP =
            Map.of(//"", "PgoAuthorize",
                    "AUTHORIZE", "PgoAuthorize",
                    "CAPTURE", "PgoCapture",
                    "RELEASE", "PgoRelease",
                    "REFUND", "PgoRefund");
    public static final Map<String, Set<String>> INTENT_STATUS_MAP =
            Map.of(
                    "AUTHORIZE", Set.of("PmntProposed", "PmntPromised"),
                    "CAPTURE", Set.of("PmntProposed", "PmntPromised", "PmntAuthorized"),
                    "RELEASE", Set.of("PmntAuthorized", "PmntDelivered", "PmntConfirmed"),
                    "REFUND", Set.of("PmntAuthorized", "PmntDelivered", "PmntConfirmed"));

    public static Map<String, Object> submitPayment(ExecutionContext ec) {
        Map<String, Object> resultMap = new HashMap<>();
        ContextStack cs = ec.getContext();
        String intent = (String) cs.get("intent");
        String path = (String) cs.get("path");
        if(intent == null || intent.isBlank() || path == null) {
            LOGGER.error("Mandatory information is missing while submitting payment: "
                    + (intent == null ? "intent" : "path") + " is NULL");
            return resultMap;
        }

        EntityValue payment = (EntityValue) cs.get("payment");
        if (payment == null) {
            String paymentId = (String) cs.get("paymentId");
            if(paymentId == null) {
                LOGGER.error("PaymentID is missing while submitting payment");
                return resultMap;
            }
            payment = ec.getEntity().find("mantle.account.payment.Payment").forUpdate(true).one();
            if(payment == null) {
                LOGGER.error("PaymentId is not found: " + paymentId);
                return resultMap;
            }
        }
        //paymentAuthCode
        String paymentAuthCode = (String) payment.getNoCheckSimple("paymentAuthCode");
        if (paymentAuthCode != null) {
            resultMap.put("paypalOrderId", paymentAuthCode);
            resultMap.put("payment", payment);
        }
        String curStatus = (String) payment.getNoCheckSimple("statusId");
        Set<String> possibleStatuses = INTENT_STATUS_MAP.get(intent);
        if(!possibleStatuses.contains(curStatus)) {
            LOGGER.error("Intent '" + intent + "' is not allowed for status '" + curStatus + "'");
            return resultMap;
        }

        String paymentId = (String) payment.getNoCheckSimple("paymentId");
        EntityValue paymentMethod = payment.findRelatedOne("toMethod", true, false);
        if(paymentMethod == null) {
            LOGGER.error("Payment with ID " + paymentId + " has no toMethod");
            return resultMap;
        }
        EntityValue paymentUom = payment.findRelatedOne("amountUom", true, false);
        Object paymentCurrency = paymentUom != null ? paymentUom.getNoCheckSimple("abbreviation") : "USD";
        String toPartyId = (String) payment.getNoCheckSimple("toPartyId");
        String accPartyId = (String) paymentMethod.getNoCheckSimple("ownerPartyId");
        if(toPartyId == null || accPartyId == null || !toPartyId.equals(accPartyId)) {
            LOGGER.error("Receiver partyId is not matched, toPartyId = " + toPartyId + " vs PaymentMethod Party ID = " + accPartyId);
            return resultMap;
        }

        String paymentMethodId = (String) paymentMethod.getNoCheckSimple("paymentMethodId");
        EntityFacade ef = ec.getEntity();
        EntityValue paypalAccount = ef.fastFindOne("mantle.account.method.PayPalAccount", true, true, paymentMethodId);
        String token = renewToken(ec, paypalAccount);
        String transactionUrl = (String) paypalAccount.getNoCheckSimple("transactionUrl");

        Map<String, Object> data = (Map<String, Object>) cs.get("data");
        if(data == null) {
            data = new HashMap<>();
        }
        data.putIfAbsent("intent", intent);
        String paypalReqId = (String) cs.get("paypalReqId");
        if(paypalReqId == null) {
            paypalReqId = paymentId + "-" + intent;
        }
        data.putIfAbsent("PayPal-Request-Id", paypalReqId);
        Map<String, Object> paymentSource = (Map<String, Object>) cs.get("paymentSource");
        if(paymentSource == null) {
            paymentSource = Map.of("paypal", Map.of());
        }
        data.putIfAbsent("payment_source", paymentSource);

        if (data.get("purchase_units") == null) {
            Map<String, Object> amountInfo = Map.of(
                    "currency_code", paymentCurrency,
                    "value", payment.getNoCheckSimple("amount"));
            Map<String, Object> unit = Map.of(
                    "amount", amountInfo
            );
            data.put("purchase_units", new Map[]{unit});
        }

        String jsonStr = JsonOutput.toJson(data);
        LOGGER.info("Requesting PayPal: " + jsonStr);
        RestClient restClient = ec.getService().rest()
                .method("POST")
                .uri(transactionUrl + path)
                .contentType("application/json")
                .acceptContentType("application/json")
                .addHeader("Authorization","Bearer " + token)
                .jsonObject(jsonStr);
        RestClient.RestResponse restResponse = restClient.call();
        int statusCode = restResponse.getStatusCode();
        boolean success = statusCode >= 200 && statusCode < 300;
        if (!success) {
            LOGGER.warn("Unsuccessful paypal request. Status code: " + statusCode + ". Response text: " + restResponse.text());
            return resultMap;
        }

        resultMap = (Map<String, Object>) restResponse.jsonObject();
        String paypalOrderId = (String) resultMap.get("id");
        if(payment.getNoCheckSimple("paymentAuthCode") == null){
            payment.put("paymentAuthCode", paypalOrderId);
        }
        String newStatus = (String) cs.get("newStatus");
        payment.put("statusId", newStatus);
        payment = payment.store();

        EntityValue responseObj = ec.getEntity().makeValue("mantle.account.method.PaymentGatewayResponse");
        responseObj.put("paymentGatewayConfigId", paymentMethod.getNoCheckSimple("paymentGatewayConfigId"));
        responseObj.put("paymentOperationEnumId", OPT_MAP.get(intent));
        responseObj.put("paymentId", payment.getNoCheckSimple("paymentId"));
        responseObj.put("paymentMethodId", payment.getNoCheckSimple("paymentMethodId"));
        responseObj.put("amount", payment.getNoCheckSimple("amount"));
        responseObj.put("amountUomId", payment.getNoCheckSimple("amountUomId"));
        responseObj.put("referenceNum", paypalOrderId);
        responseObj.put("approvalCode", "");
        responseObj.put("responseCode", statusCode);
        responseObj.put("reasonCode", "");
        responseObj.put("reasonMessage", "");
        responseObj.put("avsResult", "");
        responseObj.put("cvResult", "");
        responseObj.put("transactionDate", ec.getUser().getNowTimestamp());
        responseObj.put("resultSuccess", success ? 'Y' : 'N');
        responseObj.put("resultDeclined", success ? 'N' : 'Y');
        responseObj.put("resultError", success ? 'N' : 'Y');
        responseObj.put("resultBadExpire", "");
        responseObj.put("resultBadCardNumber", "");
        responseObj.put("resultNsf", "");
        responseObj.setSequencedIdPrimary();
        responseObj.create();

        resultMap.put("paymentGatewayResponseId", responseObj.getNoCheckSimple("paymentGatewayResponseId"));
        resultMap.put("paypalOrderId", paypalOrderId);
        resultMap.put("payment", payment);
        return resultMap;
    }
    public static String renewToken(ExecutionContext ec, EntityValue paypalAccount) {
        Timestamp now = ec.getUser().getNowTimestamp();
        Timestamp expiryDate = (Timestamp) paypalAccount.getNoCheckSimple("expiryDate");
        if(expiryDate != null && expiryDate.after(now)) {
            String token = (String) paypalAccount.getNoCheckSimple("expressCheckoutToken");
            if(token != null) {
                return token;
            }
        }

        String transactionUrl = (String) paypalAccount.getNoCheckSimple("transactionUrl");
        if(transactionUrl == null) {
            LOGGER.info("Paypal transaction url is missing");
            return null;
        }
        transactionUrl = transactionUrl + "/v1/oauth2/token";

        String authToken = (String) paypalAccount.getNoCheckSimple("authToken");
        if(authToken == null) {
            String clientId = (String) paypalAccount.getNoCheckSimple("clientId");
            String secret = (String) paypalAccount.getNoCheckSimple("secret");
            authToken = clientId + ':' + secret;
            authToken = Base64.getEncoder().encodeToString(authToken.getBytes());
            paypalAccount.set("authToken", authToken);
        }
        authToken = "Basic " + authToken;

        RestClient restClient = ec.getService().rest()
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", "en_US")
                .method("POST")
                .uri(transactionUrl)
                .addBodyParameter("grant_type","client_credentials")
                .addHeader(HttpHeader.AUTHORIZATION.name(), authToken);
        RestClient.RestResponse restResponse = restClient.call();
        Map<String, Object> responseMap = (Map<String, Object>) restResponse.jsonObject();
        int statusCode = restResponse.getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            LOGGER.warn("Can not retrieve Paypal access token. Status code = " + statusCode);
            return null;
        }
        String token = (String) responseMap.get("access_token");
        paypalAccount.set("expressCheckoutToken", token);

        //String nonce = (String) responseMap.get("nonce");
        int lifeTime = (Integer) responseMap.get("expires_in");
        Timestamp expiredDate = new Timestamp(lifeTime * 1000L + now.getTime());
        paypalAccount.set("expiryDate", expiredDate);
        paypalAccount.store();
        return token;
    }

}
