package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class AmountBreakDown extends DTOBase {
    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>>  CONVERTER_MAP =
            Map.of("name", UnitAmount::new,
                    "item_total", UnitAmount::new,
                    "shipping", UnitAmount::new,
                    "handling", UnitAmount::new,
                    "insurance", UnitAmount::new,
                    "tax_total", UnitAmount::new,
                    "shipping_discount", UnitAmount::new,
                    "discount", UnitAmount::new);
    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return CONVERTER_MAP;
    }

    public AmountBreakDown() {
        super();
    }

    public AmountBreakDown(Map<String, Object> data) {
        super(data);
    }

    /**
     * The subtotal for all items. Required if the request includes purchase_units[].items[].unit_amount.
     * Must equal the sum of (items[].unit_amount * items[].quantity) for all items.
     * item_total.value can not be a negative number.
     * @return
     */
    public UnitAmount getItemTotal() {
        return (UnitAmount) this.get("item_total");
    }

    /**
     * The shipping fee for all items within a given purchase_unit. shipping.value can not be a negative number.
     * @return
     */
    public UnitAmount getShipping() {
        return (UnitAmount) this.get("shipping"); }

    /**
     * The handling fee for all items within a given purchase_unit.
     * handling.value can not be a negative number.
     * @return
     */
    public UnitAmount getHandling() {
        return (UnitAmount) this.get("handling"); }

    /**
     * The insurance fee for all items within a given purchase_unit.
     * insurance.value can not be a negative number.
     * @return
     */
    public UnitAmount getInsurance() {
        return (UnitAmount) this.get("insurance"); }

    /**
     * The total tax for all items. Required if the request includes purchase_units.items.tax.
     * Must equal the sum of (items[].tax * items[].quantity) for all items.
     * tax_total.value can not be a negative number.
     * @return
     */
    public UnitAmount getTaxTotal() {
        return (UnitAmount) this.get("tax_total"); }

    /**
     * The shipping discount for all items within a given purchase_unit. shipping_discount.value can not be a negative number.
     * @return
     */
    public UnitAmount getShippingDiscount() {
        return (UnitAmount) this.get("shipping_discount"); }

    /**
     * The discount for all items within a given purchase_unit. discount.value can not be a negative number.
     * @return
     */
    public UnitAmount getDiscount() {
        return (UnitAmount) this.get("discount"); }
    /**
     * The subtotal for all items. Required if the request includes purchase_units[].items[].unit_amount.
     * Must equal the sum of (items[].unit_amount * items[].quantity) for all items.
     * item_total.value can not be a negative number.
     * @param itemTotal
     * @return
     */
    public void setItemTotal(UnitAmount itemTotal) { this.put("item_total", itemTotal);
    }

    /**
     * The shipping fee for all items within a given purchase_unit. shipping.value can not be a negative number.
     * @param shipping
     * @return
     */
    public void setShipping(UnitAmount shipping) { this.put("shipping", shipping);
    }

    /**
     * The handling fee for all items within a given purchase_unit.
     * handling.value can not be a negative number.
     * @param handling
     * @return
     */
    public void setHandling(UnitAmount handling) { this.put("handling", handling);
    }

    /**
     * The insurance fee for all items within a given purchase_unit.
     * insurance.value can not be a negative number.
     * @param insurance
     * @return
     */
    public void setInsurance(UnitAmount insurance) { this.put("insurance", insurance);
    }

    /**
     * The total tax for all items. Required if the request includes purchase_units.items.tax.
     * Must equal the sum of (items[].tax * items[].quantity) for all items.
     * tax_total.value can not be a negative number.
     * @param taxTotal
     * @return
     */
    public void setTaxTotal(UnitAmount taxTotal) { this.put("tax_total", taxTotal);
    }

    /**
     * The shipping discount for all items within a given purchase_unit. shipping_discount.value can not be a negative number.
     * @param shippingDiscount
     * @return
     */
    public void setShippingDiscount(UnitAmount shippingDiscount) { this.put("shipping_discount", shippingDiscount);
    }

    /**
     * The discount for all items within a given purchase_unit. discount.value can not be a negative number.
     * @param discount
     * @return
     */
    public void setDiscount(UnitAmount discount) { this.put("discount", discount);
    }

}
