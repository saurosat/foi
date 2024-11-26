package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PaymentSource extends DTOBase {
    public PaymentSource(PayerBase.Type type) {
        super(Map.of(type.value, new HashMap<String, Object>()));
    }

    public PaymentSource(Map<String, Object> data) {
        super(data);
    }

    public PayerPayPal addPayPalPayer(PayerPayPal payer) {
        if(payer == null) {
            payer = new PayerPayPal();
        }
        return (PayerPayPal) this.put(PayerBase.Type.PAYPAL.value, payer);
    }
    public PayerCard addCardPayer(PayerCard payer) {
        if(payer == null) {
            payer = new PayerCard();
        }
        return (PayerCard) this.put(PayerBase.Type.CARD.value, payer);
    }
    public PayerBase getPayer(PayerBase.Type type) {
        return (PayerBase) this.get(type.value);
    }
    public PayerBase[] getPayers() {
        PayerBase.Type[] types = PayerBase.Type.values();
        PayerBase[] payers = new PayerBase[types.length];
        for(int i = 0; i < types.length; i++) {
            payers[i] = getPayer(types[i]);
        }
        return payers;
    }
    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return MAP_CONVERTERS;
    }
    private static final Map<String, Function<Map<String, Object>, ? extends DTOBase>> MAP_CONVERTERS =
            Map.of("card", PayerCard::new,
                    "paypal", PayerPayPal::new);
}
