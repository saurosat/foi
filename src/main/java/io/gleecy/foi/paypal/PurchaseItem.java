package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class PurchaseItem extends DTOBase {
    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>> MAP_CONVERTERS = Map.of(
            "unit_amount", UnitAmount::new,
            "tax", UnitAmount::new,
            "upc", UPC::new
    );

    public PurchaseItem() {
        super();
    }

    public PurchaseItem(Map<String, Object> data) {
        super(data);
    }

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }

    /**
     * DIGITAL_GOODS	Goods that are stored, delivered, and used in their electronic format. This value is not currently supported for API callers that leverage the PayPal for Commerce Platform product.
     * PHYSICAL_GOODS	A tangible item that can be shipped with proof of delivery.
     * DONATION	A contribution or gift for which no good or service is exchanged, usually to a not for profit organization.
     */
    public enum Category {
        DIGITAL_GOODS, PHYSICAL_GOODS, DONATION
    }
    /**
     * string [ 1 .. 127 ] characters
     * The item name or title.
     * @return
     */
    public String getName() {
        return (String) this.get("name"); }

    /**
     * string <= 10 characters
     * The item quantity. Must be a whole number.
     * @return
     */
    public String getQuantity() {
        return (String) this.get("quantity"); }

    /**
     * string <= 127 characters
     * The detailed item description.
     * @return
     */
    public String getDescription() {
        return (String) this.get("description"); }

    /**
     * string <= 127 characters
     * The stock keeping unit (SKU) for the item.
     * @return
     */
    public String getSku() {
        return (String) this.get("sku"); }

    /**
     * string [ 1 .. 2048 ] characters
     * The URL to the item being purchased. Visible to buyer and used in buyer experiences.
     * @return
     */
    public String getUrl() {
        return (String) this.get("url"); }

    /**
     * The item category type.
     * @return
     */
    public String getCategory() {
        return (String) this.get("category"); }

    /**
     * string [ 1 .. 2048 ] characters ^(https:)([/|.|\w|\s|-])*\.(?:jpg|gif|png|jpeg|JPG|GIF|PNG|JPEG)Hide pattern
     * The URL of the item's image. File type and size restrictions apply. An image that violates these restrictions will not be honored.
     * @return
     */
    public String getImageUrl() {
        return (String) this.get("image_url"); }

    /**
     * The item price or rate per unit. If you specify unit_amount, purchase_units[].amount.breakdown.item_total is
     * required. Must equal unit_amount * quantity for all items. unit_amount.value can not be a negative number.
     * @return
     */
    public UnitAmount getUnitAmount() {
        return (UnitAmount) this.get("unit_amount"); }

    /**
     * The item tax for each unit. If tax is specified, purchase_units[].amount.breakdown.tax_total is required.
     * Must equal tax * quantity for all items. tax.value can not be a negative number.
     * @return
     */
    public UnitAmount getTax() {
        return (UnitAmount) this.get("tax"); }

    /**
     * The Universal Product Code of the item.
     * @return
     */
    public UPC getUPC() {
        return (UPC) this.get("upc"); }

    /**
     * string [ 1 .. 127 ] characters
     * The item name or title.
     * @param name
     * @return
     */
    public void setName(String name) { this.put("name", name);
    }

    /**
     * string <= 10 characters
     * The item quantity. Must be a whole number.
     * @param quantity
     * @return
     */
    public void setQuantity(String quantity) { this.put("quantity", quantity);
    }

    /**
     * string <= 127 characters
     * The detailed item description.
     * @param description
     * @return
     */
    public void setDescription(String description) { this.put("description", description);
    }

    /**
     * string <= 127 characters
     * The stock keeping unit (SKU) for the item.
     * @param sku
     * @return
     */
    public void setSku(String sku) { this.put("sku", sku);
    }

    /**
     * string [ 1 .. 2048 ] characters
     * The URL to the item being purchased. Visible to buyer and used in buyer experiences.
     * @param url
     * @return
     */
    public void setUrl(String url) { this.put("url", url);
    }

    /**
     * The item category type.
     * @param category
     * @return
     */
    public void setCategory(Category category) { this.put("category", category.name());
    }

    /**
     * string [ 1 .. 2048 ] characters ^(https:)([/|.|\w|\s|-])*\.(?:jpg|gif|png|jpeg|JPG|GIF|PNG|JPEG)Hide pattern
     * The URL of the item's image. File type and size restrictions apply. An image that violates these restrictions will not be honored.
     * @param imageUrl
     * @return
     */
    public void setImageUrl(String imageUrl) { this.put("image_url", imageUrl);
    }

    /**
     * The item price or rate per unit. If you specify unit_amount, purchase_units[].amount.breakdown.item_total is
     * required. Must equal unit_amount * quantity for all items. unit_amount.value can not be a negative number.
     * @param unitAmount
     * @return
     */
    public void setUnitAmount(UnitAmount unitAmount) { this.put("unit_amount", unitAmount);
    }

    /**
     * The item tax for each unit. If tax is specified, purchase_units[].amount.breakdown.tax_total is required.
     * Must equal tax * quantity for all items. tax.value can not be a negative number.
     * @param tax
     * @return
     */
    public void setTax(UnitAmount tax) { this.put("tax", tax);
    }

    /**
     * The Universal Product Code of the item.
     * @param upc
     * @return
     */
    public void setUPC(UPC upc) { this.put("upc", upc);
    }
}
