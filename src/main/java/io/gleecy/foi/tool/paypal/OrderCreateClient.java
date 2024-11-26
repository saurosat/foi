package io.gleecy.foi.tool.paypal;

import io.gleecy.foi.util.DTOBase;
import io.gleecy.foi.paypal.*;
import io.gleecy.foi.tool.Http;
import org.moqui.util.RestClient;

import java.util.List;
import java.util.Map;

public class OrderCreateClient extends PayPalClient<DTOBase, PayPalResponse>{
    public final List<PurchaseUnit> purchaseUnits;
    public OrderCreateClient(RestClient.RequestFactory requestFactory, String baseUrl, String token) {
        super(requestFactory, baseUrl, token);
        this.requestData = new DTOBase();
        this.withRequestData("purchase_units", List.of()); //initialize an arraylist of purchase_units
        this.purchaseUnits = (List<PurchaseUnit>) this.requestData.get("purchase_units");
    }

    @Override
    protected PayPalResponse newResponse(RestClient.RestResponse response) {
        return new PayPalResponse(response);
    }

    public void addPurchaseUnit(PurchaseUnit purchaseUnit) {
        this.purchaseUnits.add(purchaseUnit);
    }
    public void setIntent(Intent intent) {
        requestData.put("intent", intent);
    }

    public void setMerchantId(String merchantId) {
        for(PurchaseUnit p : this.purchaseUnits) {
            Merchant m = p.getPayee();
            if(m == null) {
                m = new Merchant();
                p.setPayee(m);
            }
            m.setMerchantId(merchantId);
        }
    }
    public void setPaymentSource(Map<String, Object> data) {
        PaymentSource paymentSource = new PaymentSource(data);
        requestData.put("payment_source", paymentSource);
    }
    public void setAuthAssertionJwt(String paypalAccountId) {
        withHeader(Http.Header.AUTH_ASSERT, createAuthAssertionJwt(paypalAccountId));
    }
    public void setPaymentSource(PayerBase.Type type) {
        requestData.put("payment_source", new PaymentSource(type));
    }
    public void setReturnUrl(String returnUrl) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setReturnUrl(returnUrl);
        }
    }
    public void setCancelUrl(String cancelUrl) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setCancelUrl(cancelUrl);
        }
    }
    public void setBrandName(String brandName) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setBrandName(brandName);
        }
    }
    public void setLocale(String locale) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setLocale(locale);
        }
    }

    public void setShippingPref(XContextBase.ShippingPref shippingPref) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setShippingPref(shippingPref);
        }
    }

    public void setUserAction(XContextBase.UserAction userAction) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setUserAction(userAction);
        }
    }
    public void setLandingPage(XContextBase.LandingPage landingPage) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setLandingPage(landingPage);
        }
    }

    public void setPaymentMethodPref(XContextBase.PaymentMethodPref paymentPref) {
        PaymentSource paymentSource = (PaymentSource) requestData.get("payment_source");
        if(paymentSource == null) return;

        PayerBase[] payers = paymentSource.getPayers();
        for(int i = 0; i < payers.length; i++) {
            if(payers[i] == null) continue;;
            XContextBase xContext = payers[i].getExperienceContext();
            xContext.setPaymentMethodPref(paymentPref);
        }
    }
}
