package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTO;
import io.gleecy.foi.util.DTOBase;
import org.moqui.util.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PayPalResponse extends DTOBase {
    public final int statusCode;
    public final boolean success;
    public PayPalResponse(RestClient.RestResponse response) {
        super((Map<String, Object>) response.jsonObject());
        statusCode = response.getStatusCode();
        success = (statusCode >= 200 && statusCode < 400);
        //response.checkError()
    }
    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }
    @Override
    protected Map<String, Function<String, ? extends DTO>> getStringConverters() {
        return STR_CONVERERS;
    }

    public String getOrderId() { return (String) this.get("id"); }
    public List<Hateoas> getLinks() {
        return (List<Hateoas>) this.get("links");
    }
    public List<PurchaseUnit> getPurchaseUnits () {
        return (List<PurchaseUnit>) this.get("purchase_units");
    }
    public PaymentSource getPaymentSource() {
        return (PaymentSource) this.get("payment_source");
    }
    public PayerPayPal getPayer() {
        return (PayerPayPal) this.get("payer");
    }
    public OrderStatus getStatus() {
        return (OrderStatus) this.get("status");
    }
    public List<ReqFieldError> getFieldErrors() { return (List<ReqFieldError>) this.get("details"); }
    public String getName() { return (String) this.get("name"); }
    public String getMessage() { return (String) this.get("message"); }
    public String getDebugId() { return  (String) this.get("debug_id"); }

    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>>  MAP_CONVERTERS =
            Map.of("purchase_units", PurchaseUnit::new
                    , "payment_source", PaymentSource::new
                    , "payer", PayerPayPal::new
                    , "links", Hateoas::new
                    , "details", ReqFieldError::new
            );
    private static final Map<String, Function<String, ? extends DTO>>  STR_CONVERERS =
            Map.of("status", OrderStatus::valueOf
            );
}
