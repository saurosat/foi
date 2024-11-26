package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PurchaseUnit extends DTOBase {
    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>> MAP_CONVERTERS = Map.of(
            "items", PurchaseItem::new,
            "payee", Merchant::new,
            "shipping", Shipping::new,
            "amount", TotalAmount::new
    );

    public PurchaseUnit() {
        super();
    }

    public PurchaseUnit(Map<String, Object> data) {
        super(data);
    }

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }

    /**
     * string [ 1 .. 256 ] characters
     * The API caller-provided external ID for the purchase unit.
     * Required for multiple purchase units when you must update the order through PATCH.
     * If you omit this value and the order contains only one purchase unit, PayPal sets this value to default.
     */
    public String getRefId() {
        return (String) this.get("reference_id"); }

    /**
     * string [ 1 .. 127 ] characters
     * The purchase description. The maximum length of the character is dependent on the type of characters used.
     * The character length is specified assuming a US ASCII character. Depending on type of character;
     * (e.g. accented character, Japanese characters) the number of characters that that can be
     * specified as input might not equal the permissible max length.
     */
    public String getDescription() {
        return (String) this.get("description"); }

    /**
     * string [ 1 .. 255 ] characters
     * The API caller-provided external ID. Used to reconcile client transactions with PayPal transactions.
     * Appears in transaction and settlement reports but is not visible to the payer.
     */
    public String getCustomId() {
        return (String) this.get("custom_id"); }

    /**
     * string [ 1 .. 127 ] characters
     * The API caller-provided external invoice number for this order.
     * Appears in both the payer's transaction history and the emails that the payer receives.
     */
    public String getInvoiceId() {
        return (String) this.get("invoice_id"); }

    /**
     * string [ 1 .. 22 ] characters
     * The soft descriptor is the dynamic text used to construct the statement descriptor that appears on a payer's card statement.
     *
     * If an Order is paid using the "PayPal Wallet", the statement descriptor will appear in following format on the payer's card statement: PAYPAL_prefix+(space)+merchant_descriptor+(space)+ soft_descriptor
     *
     * Note: The merchant descriptor is the descriptor of the merchant’s payment receiving preferences which can be seen by logging into the merchant account https://www.sandbox.paypal.com/businessprofile/settings/info/edit
     * The PAYPAL prefix uses 8 characters. Only the first 22 characters will be displayed in the statement.
     * For example, if:
     * The PayPal prefix toggle is PAYPAL *.
     * The merchant descriptor in the profile is Janes Gift.
     * The soft descriptor is 800-123-1234.
     * Then, the statement descriptor on the card is PAYPAL * Janes Gift 80.
     */
    public String getSoftDescriptor() {
        return (String) this.get("soft_descriptor"); }

    /**
     * An array of items that the customer purchases from the merchant.
     */
    public List<PurchaseItem> getItems() {
        return  (List<PurchaseItem>) this.get("items");
    }

    /**
     * The merchant who receives payment for this transaction.
     */
    public Merchant getPayee() {
        return (Merchant) this.get("payee"); }

    /**
     * The name and address of the person to whom to ship the items
     */
    public Shipping getShipping() {
        return (Shipping) this.get("shipping"); }

    /**
     * object
     * The total order amount with an optional breakdown that provides details, such as the total item amount, total tax amount, shipping, handling, insurance, and discounts, if any.
     * If you specify amount.breakdown, the amount equals item_total plus tax_total plus shipping plus handling plus insurance minus shipping_discount minus discount.
     * The amount must be a positive number. The amount.value field supports up to 15 digits preceding the decimal. For a list of supported currencies, decimal precision, and maximum charge amount, see the PayPal REST APIs
     * <a href="https://developer.paypal.com/api/rest/reference/currency-codes/">...</a>
     * @param amount
     * @return
     */
    public TotalAmount setAmount(TotalAmount amount) {
        return (TotalAmount) this.put("amount", amount);
    }

    /**
     * object
     * The total order amount with an optional breakdown that provides details, such as the total item amount, total tax amount, shipping, handling, insurance, and discounts, if any.
     * If you specify amount.breakdown, the amount equals item_total plus tax_total plus shipping plus handling plus insurance minus shipping_discount minus discount.
     * The amount must be a positive number. The amount.value field supports up to 15 digits preceding the decimal. For a list of supported currencies, decimal precision, and maximum charge amount, see the PayPal REST APIs
     * <a href="https://developer.paypal.com/api/rest/reference/currency-codes/">...</a>
     * @return
     */
    public TotalAmount getAmount() {
        return (TotalAmount) this.get("amount");
    }
    /**
     * string [ 1 .. 256 ] characters
     * The API caller-provided external ID for the purchase unit.
     * Required for multiple purchase units when you must update the order through PATCH.
     * If you omit this value and the order contains only one purchase unit, PayPal sets this value to default.
     * @param refId
     * @return
     */
    public void setRefId(String refId) { this.put("reference_id", refId);
    }

    /**
     * string [ 1 .. 127 ] characters
     * The purchase description. The maximum length of the character is dependent on the type of characters used.
     * The character length is specified assuming a US ASCII character. Depending on type of character;
     * (e.g. accented character, Japanese characters) the number of characters that that can be
     * specified as input might not equal the permissible max length.
     * @param description
     * @return
     */
    public void setDescription(String description) { this.put("description", description);
    }

    /**
     * string [ 1 .. 255 ] characters
     * The API caller-provided external ID. Used to reconcile client transactions with PayPal transactions.
     * Appears in transaction and settlement reports but is not visible to the payer.
     * @param customId
     * @return
     */
    public void setCustomId(String customId) { this.put("custom_id", customId);
    }

    /**
     * string [ 1 .. 127 ] characters
     * The API caller-provided external invoice number for this order.
     * Appears in both the payer's transaction history and the emails that the payer receives.
     * @param invoiceId
     * @return
     */
    public void setInvoiceId(String invoiceId) { this.put("invoice_id", invoiceId);
    }

    /**
     * string [ 1 .. 22 ] characters
     * The soft descriptor is the dynamic text used to construct the statement descriptor that appears on a payer's card statement.
     *
     * If an Order is paid using the "PayPal Wallet", the statement descriptor will appear in following format on the payer's card statement: PAYPAL_prefix+(space)+merchant_descriptor+(space)+ soft_descriptor
     *
     * Note: The merchant descriptor is the descriptor of the merchant’s payment receiving preferences which can be seen by logging into the merchant account https://www.sandbox.paypal.com/businessprofile/settings/info/edit
     * The PAYPAL prefix uses 8 characters. Only the first 22 characters will be displayed in the statement.
     * For example, if:
     * The PayPal prefix toggle is PAYPAL *.
     * The merchant descriptor in the profile is Janes Gift.
     * The soft descriptor is 800-123-1234.
     * Then, the statement descriptor on the card is PAYPAL * Janes Gift 80.
     * @param softDescriptor
     * @return
     */
    public void setSoftDescriptor(String softDescriptor) { this.put("soft_descriptor", softDescriptor);
    }

    /**
     * An array of items that the customer purchases from the merchant.
     * @param items
     * @return
     */
    public List<PurchaseItem> setItems(List<PurchaseItem> items) {
        return (List<PurchaseItem>) this.put("items", items);
    }

    /**
     * The merchant who receives payment for this transaction.
     * @param payee
     * @return
     */
    public void setPayee(Merchant payee) { this.put("payee", payee);
    }

    /**
     * The name and address of the person to whom to ship the items
     * @param shipping
     * @return
     */
    public void setShipping(Shipping shipping) { this.put("shipping", shipping);
    }
}
