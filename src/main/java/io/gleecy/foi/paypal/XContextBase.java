package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

public class XContextBase extends DTOBase {
    public XContextBase() {
        super();
    }

    public XContextBase(Map<String, Object> data) {
        super(data);
    }

    /**
     * string
     * The URL where the customer will be redirected upon approving a payment.
     * @param return_url
     * @return
     */
    public String setReturnUrl(String return_url) {
        return (String) this.put("return_url", return_url);
    }

    /**
     * string
     * The URL where the customer will be redirected upon cancelling the payment approval.
     * @param cancel_url
     * @return
     */
    public String setCancelUrl(String cancel_url) { this.put("cancel_url", cancel_url);
        return cancel_url;
    }


    /**
     * brand_name
     * string [ 1 .. 127 ] characters
     * The label that overrides the business name in the PayPal account on the PayPal site.
     * The pattern is defined by an external party and supports Unicode.
     * @param brandName
     * @return
     */
    public void setBrandName(String brandName) {}

    /**
     * shipping_preference
     * Default: "GET_FROM_FILE"
     * The location from which the shipping address is derived.
     * @param shippingPref
     * @return
     */
    public void setShippingPref(XContextPayPal.ShippingPref shippingPref) {}
    /**
     * landing_page
     * string [ 1 .. 13 ] characters
     * Default: "NO_PREFERENCE"
     * The type of landing page to show on the PayPal site for customer checkout.
     * @param landingPage
     * @return
     */
    public void setLandingPage(XContextPayPal.LandingPage landingPage) {}

    /**
     * user_action
     * string [ 1 .. 8 ] characters
     * Default: "CONTINUE"
     * Configures a Continue or Pay Now checkout flow.
     * @param userAction
     * @return
     */
    public void setUserAction(XContextPayPal.UserAction userAction) {}

    /**
     * payment_method_preference
     * string [ 1 .. 255 ] characters
     * Default: "UNRESTRICTED"
     * The merchant-preferred payment methods.
     * @param paymentMethodPref
     * @return
     */
    public void setPaymentMethodPref(XContextPayPal.PaymentMethodPref paymentMethodPref) {}

    /**
     * locale
     * string [ 2 .. 10 ] characters ^[a-z]{2}(?:-[A-Z][a-z]{3})?(?:-(?:[A-Z]{2}|[0-9]{3}))?$
     * The BCP 47-formatted locale of pages that the PayPal payment experience shows. PayPal supports a five-character code. For example, da-DK, he-IL, id-ID, ja-JP, no-NO, pt-BR, ru-RU, sv-SE, th-TH, zh-CN, zh-HK, or zh-TW.
     * @param locale
     * @return
     */
    public void setLocale(String locale) {}

    public enum UserAction {

        /**
         * After you redirect the customer to the PayPal payment page, a Continue button appears.
         * Use this option when the final amount is not known when the checkout flow is initiated and you want
         * to redirect the customer to the merchant page without processing the payment.
         */
        CONTINUE,
        /**
         * After you redirect the customer to the PayPal payment page, a Pay Now button appears.
         * Use this option when the final amount is known when the checkout is initiated and you want
         * to process the payment immediately when the customer clicks Pay Now.
         */
        PAY_NOW
    }
    public enum PaymentMethodPref {
        /**
         * Accepts any type of payment from the customer.
         */
        UNRESTRICTED,
        /**
         * Accepts only immediate payment from the customer. For example, credit card, PayPal balance, or instant ACH.
         * Ensures that at the time of capture, the payment does not have the pending status.
         */
        IMMEDIATE_PAYMENT_REQUIRED
    }
    public enum ShippingPref {
        /**
         * Get the customer-provided shipping address on the PayPal site.
         */
        GET_FROM_FILE,
        /**
         * Removes the shipping address information from the API response and the Paypal site.
         * However, the shipping.phone_number and shipping.email_address fields will still be returned to allow for digital goods delivery.
         */
        NO_SHIPPING,
        /**
         * Get the merchant-provided address. The customer cannot change this address on the PayPal site.
         * If merchant does not pass an address, customer can choose the address on PayPal pages.
         */
        SET_PROVIDED_ADDRESS
    }
    public enum LandingPage {
        /**
         * When the customer clicks PayPal Checkout, the customer is redirected to a page to log in to PayPal and approve the payment.
         */
        LOGIN,
        /**
         * When the customer clicks PayPal Checkout, the customer is redirected to a page to enter credit or debit card and other relevant billing information required to complete the purchase.
         * This option has previously been also called as 'BILLING'
         */
        GUEST_CHECKOUT,
        /**
         * When the customer clicks PayPal Checkout, the customer is redirected to either a page to log in to PayPal
         * and approve the payment or to a page to enter credit or debit card and other relevant billing information
         * required to complete the purchase, depending on their previous interaction with PayPal.
         */
        NO_PREFERENCE
    }
}
