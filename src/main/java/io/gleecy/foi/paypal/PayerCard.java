package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class PayerCard extends PayerBase{
    public PayerCard() {
        super();
    }

    public PayerCard(Map<String, Object> data) {
        super(data);
    }

    public XContextBase setXContext(XContextBase xContext) {
        return (XContextBase) this.put("experience_context", xContext);
    }
    public XContextBase getXContext(XContextBase xContext) {
        return (XContextBase) this.get("experience_context");
    }

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }

    private final Map<String, Function<Map<String, Object>, ? extends DTOBase>> MAP_CONVERTERS =
            Map.of( "experience_context", XContextBase::new,
                    "billing_address", Address::new);

}
