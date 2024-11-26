package io.gleecy.foi.util;

import org.moqui.entity.EntityFacade;
import org.moqui.entity.EntityList;
import org.moqui.entity.EntityValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EntityUtil {
    public static double[] getItemTotalPrice(EntityValue orderItem) {
        BigDecimal oAmount = (BigDecimal) orderItem.getNoCheckSimple("selectedAmount");
        double amount = oAmount == null ? 1.0 : oAmount.doubleValue();

        BigDecimal oQuantity = (BigDecimal) orderItem.getNoCheckSimple("quantity");
        double quantity = oQuantity == null ? 1.0 : oQuantity.doubleValue();
        quantity = quantity * amount;

        BigDecimal unitAmount = (BigDecimal) orderItem.getNoCheckSimple("unitAmount");
        double unitPrice = unitAmount == null ? 0.0 : unitAmount.doubleValue();

        double total = new BigDecimal(quantity * unitPrice)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
        return new double[]{total, quantity, unitPrice};
    }
    public static String getFoProductUrl(Map<String, Object> productStore, String pseudoId) {
        return "https://localhost/getFoProductUrl";
    }
    public static String getFoProductImageUrl(Map<String, Object> productStore, String pseudoId) {
        return "https://localhost/getFoProductImageUrl/someImage.jpg";
    }
    public static Set<String> getOrderItemProductTypes(EntityFacade ef) {
        EntityList typeList = ef.find("moqui.basic.EnumGroupMember")
                .selectField("enumId")
                .condition("enumGroupEnumId", "EngItemsProduct")
                .useCache(true).disableAuthz().forUpdate(false)
                .list();
        return typeList.stream()
                .map(entity -> (String) entity.getNoCheckSimple("enumId"))
                .collect(Collectors.toUnmodifiableSet());
    }
}
