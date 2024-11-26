package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

public class Address extends DTOBase {
    public Address() {
        super();
    }

    public Address(Map<String, Object> data) {
        super(data);
    }

    /**
     * string <= 300 characters
     * The first line of the address, such as number and street, for example, 173 Drury Lane.
     * Needed for data entry, and Compliance and Risk checks.
     * This field needs to pass the full address.
     */
    public String getAddressLine1() {
        return (String) this.get("address_line_1"); }

    /**
     *
     * string <= 300 characters
     * The second line of the address, for example, a suite or apartment number.
     */
    public String getaddressLine2() {
        return (String) this.get("address_line_2"); }

    /**
     * string <= 300 characters
     * The highest-level sub-division in a country, which is usually a province, state, or ISO-3166-2 subdivision.
     * This data is formatted for postal delivery, for example, CA and not California. Value, by country, is:
     *
     * UK. A county.
     * US. A state.
     * Canada. A province.
     * Japan. A prefecture.
     * Switzerland. A kanton.
     */
    public String getAdminArea1() {
        return (String) this.get("admin_area_1"); }

    /**
     *
     * string <= 120 characters
     * A city, town, or village. Smaller than admin_area_level_1.
     * @return
     */
    public String getAdminArea2() {
        return (String) this.get("admin_area_2"); }

    /**
     * string <= 60 characters
     * The postal code, which is the ZIP code or equivalent.
     * Typically required for countries with a postal code or an equivalent.
     * See <a href="https://en.wikipedia.org/wiki/Postal_code">...</a>
     * @return
     */
    public String getPostalCode() {
        return (String) this.get("postal_code"); }

    /**
     * string = 2 characters ^([A-Z]{2}|C2)$
     * The 2-character ISO 3166-1 code that identifies the country or region.
     * <a href="https://developer.paypal.com/api/rest/reference/country-codes/">...</a>
     * Note: The country code for Great Britain is GB and not UK as used in the top-level domain names for that country. Use the C2 country code for China worldwide for comparable uncontrolled price (CUP) method, bank card, and cross-border transactions.
     * @return
     */
    public String getCountryCode() {
        return (String) this.get("country_code"); }
    /**
     * string <= 300 characters
     * The first line of the address, such as number and street, for example, 173 Drury Lane.
     * Needed for data entry, and Compliance and Risk checks.
     * This field needs to pass the full address.
     * @param addressLine1
     * @return
     */
    public void setAddressLine1(String addressLine1) { this.put("address_line_1", addressLine1);
    }

    /**
     *
     * string <= 300 characters
     * The second line of the address, for example, a suite or apartment number.
     * @param addressLine2
     * @return
     */
    public void setAddressLine2(String addressLine2) { this.put("address_line_2", addressLine2);
    }

    /**
     * string <= 300 characters
     * The highest-level sub-division in a country, which is usually a province, state, or ISO-3166-2 subdivision.
     * This data is formatted for postal delivery, for example, CA and not California. Value, by country, is:
     *
     * UK. A county.
     * US. A state.
     * Canada. A province.
     * Japan. A prefecture.
     * Switzerland. A kanton.
     * @param adminArea1
     * @return
     */
    public void setAdminArea1(String adminArea1) { this.put("admin_area_1", adminArea1);
    }

    /**
     *
     * string <= 120 characters
     * A city, town, or village. Smaller than admin_area_level_1.
     * @param adminArea2
     * @return
     */
    public void setAdminArea2(String adminArea2) { this.put("admin_area_2", adminArea2);
    }

    /**
     * string <= 60 characters
     * The postal code, which is the ZIP code or equivalent.
     * Typically required for countries with a postal code or an equivalent.
     * See <a href="https://en.wikipedia.org/wiki/Postal_code">...</a>
     * @param postalCode
     * @return
     */
    public void setPostalCode(String postalCode) { this.put("postal_code", postalCode);
    }

    /**
     * string = 2 characters ^([A-Z]{2}|C2)$
     * The 2-character ISO 3166-1 code that identifies the country or region.
     * <a href="https://developer.paypal.com/api/rest/reference/country-codes/">...</a>
     * Note: The country code for Great Britain is GB and not UK as used in the top-level domain names for that country. Use the C2 country code for China worldwide for comparable uncontrolled price (CUP) method, bank card, and cross-border transactions.
     * @param countryCode
     * @return
     */
    public void setCountryCode(String countryCode) { this.put("country_code", countryCode);
    }

}
