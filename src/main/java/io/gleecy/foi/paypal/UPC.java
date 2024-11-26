package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

/**
 * The Universal Product Code
 **/
public class UPC extends DTOBase {
    public UPC(Map<? extends String, ?> data) {
        super(data);
    }

    /**
     * string [ 1 .. 5 ] characters ^[0-9A-Z_-]+$
     * The Universal Product Code type.
     */
    public enum Type {
        UPCA ("UPC-A"), UPCB("UPC-B"), UPCC("UPC-C"), UPCD("UPC-D"), UPCE("UPC-E"),
        UPC2("UPC-2"), UPC5("UPC-5");
        public final String value;
        private Type(String value) { this.value = value; }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * The Universal Product Code type.
     * @param type
     * @return
     */
    public void setType(Type type) { this.put("type", type.value);
    }

    /**
     * string [ 6 .. 17 ] characters
     * The UPC product code of the item.
     * @param code
     * @return
     */
    public void setCode(String code) { this.put("code", code);
    }
}
