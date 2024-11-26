package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

public class Merchant extends DTOBase {
    public Merchant() {
        super();
    }
    public Merchant(String merchantId, String email) {
        super();
        setMerchantId(merchantId);
        setEmail(email);
    }

    public Merchant(Map<String, Object> data) {
        super(data);
    }

    /**
     * string = 13 characters ^[2-9A-HJ-NP-Z]{13}$
     * The encrypted PayPal account ID of the merchant.
     * @param merchantId
     * @return
     */
    public void setMerchantId(String merchantId) {
        this.put("merchant_id", merchantId);
    }
    public String getMerchantId() {
        return (String) this.get("merchant_id");
    }

    /**
     * string [ 3 .. 254 ] characters (?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-zA-Z0-9-]*[a-zA-Z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])Hide pattern
     * The email address of merchant.
     * @param email
     * @return
     */
    public void setEmail(String email) { this.put("email_address", email);
    }
    public String getEmail() {
        return (String) this.get("email_address");
    }
}
