package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

/**
 * The item price or rate per unit.
 */
public class UnitAmount extends DTOBase {
    public UnitAmount() {
        super();
    }
    public UnitAmount(String amount, String currencyCode) {
        super();
        setValue(amount);
        setCurrencyCode(currencyCode);
    }

    public UnitAmount(Map<String, Object> data) {
        super(data);
    }

    /**
     * string = 3 characters
     * The three-character ISO-4217 currency code that identifies the currency.
     * @param currencyCode
     * @return
     */
    public void setCurrencyCode(String currencyCode) { this.put("currency_code", currencyCode);
    }

    /**
     * string <= 32 characters
     * The value, which might be:
     *
     * An integer for currencies like JPY that are not typically fractional.
     * A decimal fraction for currencies like TND that are subdivided into thousandths.
     * For the required number of decimal places for a currency code,
     * see https://developer.paypal.com/api/rest/reference/currency-codes/
     * @param value
     * @return
     */
    public void setValue(String value) { this.put("value", value);
    }

}
