package io.gleecy.foi.paypal;

/**
 * Purchase item category types
 */
public enum ItemCategory {
    /**
     * Goods that are stored, delivered, and used in their electronic format.
     * This value is not currently supported for API callers that leverage the PayPal for Commerce Platform product.
     */
    DIGITAL_GOODS,
    /**
     * 	A tangible item that can be shipped with proof of delivery.
     */
    PHYSICAL_GOODS,
    /**
     * A contribution or gift for which no good or service is exchanged, usually to a not for profit organization.
     */
    DONATION;
    public static ItemCategory fromString(String value) {
        if(value == null) return null;
        value = value.trim().toUpperCase();
        try {
            return ItemCategory.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
