package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

public class PhoneNumber extends DTOBase {
    public PhoneNumber() {
        super();
    }

    public PhoneNumber(Map<? extends String, ?> data) {
        super(data);
    }

    public String setCountryCode(String countryCode) {
        if(countryCode != null) {
            countryCode = countryCode.trim();
            if(countryCode.startsWith("0")) {
                countryCode = "+" + countryCode.substring(1);
            }
        }
        return (String) this.put("country_code", countryCode);
    }
    public String getCountryCode() {
        return (String) this.get("country_code");
    }
    public String setNationalNumber(String nationalNumber) {
        if(nationalNumber != null) {
            nationalNumber = nationalNumber.replaceAll(" ", "");
        }
        return (String) this.put("national_number", nationalNumber);
    }
    public String getNationalNumber() {
        return (String) this.get("national_number");
    }
}
