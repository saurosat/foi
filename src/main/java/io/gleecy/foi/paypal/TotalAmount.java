package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class TotalAmount extends UnitAmount{
    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>> MAP_CONVERTERS = Map.of(
            "breakdown", AmountBreakDown::new
    );

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }

    public TotalAmount() {
        super();
    }
    public TotalAmount(String value, String currencyCode) {
        super(value, currencyCode);
    }

    public TotalAmount(Map<String, Object> data) {
        super(data);
    }

    public void setBreakDown(AmountBreakDown breakdown) {
        this.put("breakdown", breakdown);
    }
    public AmountBreakDown getBreakDown() {
        return (AmountBreakDown) this.get("breakdown");
    }

}
