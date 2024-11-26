package io.gleecy.foi.tool;

import java.util.Map;

public final class Http {
    public enum ContentType{
        JSON("application/json"), FORM_URL_ENCODED("application/x-www-form-urlencoded");
        public final String value;
        private ContentType(String value) {
            this.value = value;
        }
        private static final Map<String, ContentType> valMap = Map.of("application/json", JSON,
                "application/x-www-form-urlencoded", FORM_URL_ENCODED);
        public static ContentType fromString(String value) {
            return  valMap.get(value);
        }
    }

    public enum Header {
        CONTENT_TYPE("Content_Type"),
        AUTHORIZATION("Authorization"),
        ACCEPT("Accept"),
        CHARSET("Accept-Charset"),
        LANGUAGE("Accept-Language"),
        PAYPAL_REQ_ID ("PayPal-Request-Id"),
        AUTH_ASSERT("PayPal-Auth-Assertion"),
        PAYPAL_PARTNER_ID ("PayPal-Partner-Attribution-Id"),
        PAYPAL_AUTH_ASSERT("PayPal-Auth-Assertion");
        public final String value;
        private Header(String value) { this.value = value;}
    }
}
