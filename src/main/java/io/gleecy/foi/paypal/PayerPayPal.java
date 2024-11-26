package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class PayerPayPal extends PayerBase {
    public PayerPayPal() {
        super();
    }

    public PayerPayPal(Map<String, Object> data) {
        super(data);
    }

    public String getEmail() { return (String) this.get("email_address"); }
    public String getPayerId() { return (String) this.get("payer_id"); }
    public String getBirthday() { return (String) this.get("birth_date"); }
    public Name getName() {
        return (Name) this.get("name");
    }
    public Map<String, Object> getPhone() {
        return (Map<String, Object>) this.get("phone");
    }
    //public Map<String, Object> getTaxInfo() { return (Map<String, Object>) this.get("tax_info"); }
    public Address getAddress() { return (Address) this.get("address"); }

    public void setExperienceContext(XContextPayPal xContext) {
        super.setExperienceContext(xContext);
    }

    @Override
    protected XContextBase createXContext() {
        return new XContextPayPal();
    }

    @Override
    public XContextPayPal getExperienceContext() {
        return (XContextPayPal) super.getExperienceContext();
    }

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return CONVERTER_MAP;
    }
    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>> CONVERTER_MAP =
            Map.of("name", Name::new,
                    "address", Address::new,
                    "experience_context", XContextPayPal::new);
}
