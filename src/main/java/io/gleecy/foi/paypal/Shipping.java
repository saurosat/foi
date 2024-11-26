package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class Shipping extends DTOBase {
    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>> MAP_CONVERTERS = Map.of(
            "address", Address::new
    );

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }

    public Shipping() {
        super();
    }

    public Shipping(Map<String, Object> data) {
        super(data);
    }

    /**
     * A classification for the method of purchase fulfillment (e.g shipping, in-store pickup, etc).
     * SHIPPING	The payer intends to receive the items at a specified address.
     * PICKUP_IN_STORE	The payer intends to pick up the item(s) from the payee's physical store. Also termed as BOPIS, "Buy Online, Pick-up in Store". Seller protection is provided with this option.
     * PICKUP_FROM_PERSON	The payer intends to pick up the item(s) from the payee in person. Also termed as BOPIP, "Buy Online, Pick-up in Person". Seller protection is not available, since the payer is receiving the item from the payee in person, and can validate the item prior to payment
     */
    public enum Type {
        /**
         * The payer intends to receive the items at a specified address.
         */
        SHIPPING,
        /**
         * The payer intends to pick up the item(s) from the payee's physical store.
         * Also termed as BOPIS, "Buy Online, Pick-up in Store". Seller protection is provided with this option.
         */
        PICKUP_FROM_PERSON,
        /**
         * The payer intends to pick up the item(s) from the payee's physical store.
         * Also termed as BOPIS, "Buy Online, Pick-up in Store". Seller protection is provided with this option.
         */
        PICKUP_IN_STORE
    }

    /**
     * A classification for the method of purchase fulfillment (e.g shipping, in-store pickup, etc).
     * Either type or options may be present, but not both.
     * @param type
     * @return
     */
    public void setType(Type type) { this.put("type", type);
    }

    /**
     * The name of the person to whom to ship the items. Supports only the full_name property.
     * @param fullName
     * @return
     */
    public Map<String, String> setName(String fullName) {
        return (Map<String, String>) this.put("name", Map.of("full_name", fullName));
    }

    /**
     * The address of the person to whom to ship the items.
     * @param address
     * @return
     */
    public void setAddress(Address address) { this.put("address", address);
    }

    public void setPhone(PhoneNumber number) {
        this.put("phone_number", number);
    }
}
