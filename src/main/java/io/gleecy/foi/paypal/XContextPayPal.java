package io.gleecy.foi.paypal;

import java.util.Map;

public class XContextPayPal extends XContextBase {
    public XContextPayPal() {
        super();
    }

    public XContextPayPal(Map<String, Object> data) {
        super(data);
    }

    /**
     * brand_name
     * string [ 1 .. 127 ] characters
     * The label that overrides the business name in the PayPal account on the PayPal site.
     * The pattern is defined by an external party and supports Unicode.
     * @param brandName
     * @return
     */
    @Override
    public void setBrandName(String brandName) {
        this.put("brand_name", brandName);
    }

    /**
     * shipping_preference
     * Default: "GET_FROM_FILE"
     * The location from which the shipping address is derived.
     * @param shippingPref
     * @return
     */
    @Override
    public void setShippingPref(ShippingPref shippingPref) {
        this.put("shipping_preference", shippingPref.name());
    }
    /**
     * landing_page
     * string [ 1 .. 13 ] characters
     * Default: "NO_PREFERENCE"
     * The type of landing page to show on the PayPal site for customer checkout.
     * @param landingPage
     * @return
     */
    @Override
    public void setLandingPage(LandingPage landingPage) {
         this.put("landing_page", landingPage.name());
    }

    /**
     * user_action
     * string [ 1 .. 8 ] characters
     * Default: "CONTINUE"
     * Configures a Continue or Pay Now checkout flow.
     * @param userAction
     * @return
     */
    @Override
    public void setUserAction(UserAction userAction) {
        this.put("user_action", userAction.name());
    }

    /**
     * payment_method_preference
     * string [ 1 .. 255 ] characters
     * Default: "UNRESTRICTED"
     * The merchant-preferred payment methods.
     * @param paymentMethodPref
     * @return
     */
    @Override
    public void setPaymentMethodPref(PaymentMethodPref paymentMethodPref) {
        this.put("payment_method_preference", paymentMethodPref.name());
    }

    /**
     * locale
     * string [ 2 .. 10 ] characters ^[a-z]{2}(?:-[A-Z][a-z]{3})?(?:-(?:[A-Z]{2}|[0-9]{3}))?$
     * The BCP 47-formatted locale of pages that the PayPal payment experience shows. PayPal supports a five-character code. For example, da-DK, he-IL, id-ID, ja-JP, no-NO, pt-BR, ru-RU, sv-SE, th-TH, zh-CN, zh-HK, or zh-TW.
     * @param locale
     * @return
     */
    @Override
    public void setLocale(String locale) {
        this.put("locale", locale);
    }
}
