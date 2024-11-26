package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class PayerBase extends DTOBase {
    public PayerBase() {
        super();
    }

    public PayerBase(Map<String, Object> data) {
        super(data);
    }

    protected XContextBase createXContext() {
        return new XContextBase();
    }
    public void setExperienceContext(XContextBase xContextBase) {
        this.put("experience_context", xContextBase);
    }
    public XContextBase getExperienceContext() {
        return (XContextBase) this.get("experience_context");
    }

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }

    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>> MAP_CONVERTERS = Map.of(
            "experience_context", XContextBase::new
    );
    public enum Type {
        CARD("card"), PAYPAL("paypal");
        public final String value;
        private Type(String value) { this.value = value; }
        private static Map<String, Type> _valMap = Map.of("card", CARD, "paypal", PAYPAL);
        public static Type fromString(String value) { return _valMap.get(value); }
    }

}
