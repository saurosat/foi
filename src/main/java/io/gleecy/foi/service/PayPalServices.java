package io.gleecy.foi.service;

import io.gleecy.foi.paypal.*;
import io.gleecy.foi.tool.Http;
import io.gleecy.foi.tool.paypal.AuthorizeClient;
import io.gleecy.foi.tool.paypal.OrderCreateClient;
import io.gleecy.foi.tool.paypal.PPClientConfig;
import io.gleecy.foi.util.EntityUtil;
import io.gleecy.foi.util.MandatoryFieldError;
import org.moqui.context.ExecutionContext;
import org.moqui.entity.EntityCondition;
import org.moqui.entity.EntityFacade;
import org.moqui.entity.EntityValue;
import org.moqui.util.ContextStack;
import org.moqui.util.ObjectUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;


public class PayPalServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayPalServices.class);

    private static final String PAYPAL_ERR_MSG = "Failed to send payment request to PayPal. Please contact us to report about this error";
    private static final String PAYPAL_SUCCESS_MSG = "Payment is successfully submitted";
    private static final Map<String, Object> PAYPAL_ERR = Map.of("success", 'N',
            "message",PAYPAL_ERR_MSG);

    private static double getItemsTotalByType(EntityFacade ef, String orderId, String orderPartSeqId, String orderItemType) {
        List<EntityValue> items = ef.find("mantle.order.OrderItem")
                .condition("orderId",orderId)
                .condition("orderPartSeqId", orderPartSeqId)
                .condition("itemTypeEnumId", orderItemType)
                .forUpdate(false).useCache(true).list();
        if(items.isEmpty()) {
            return -1;
        }
        return items.stream()
                .map(EntityUtil::getItemTotalPrice)
                .map(arr -> arr[0])
                .reduce(0.0, Double::sum);
    }
    private static double[] buildPurchaseItems(List<PurchaseItem> purchaseItems,
                                               EntityFacade ef, Map<String, Object> productStore,
                                               String orderId, String orderPartSeqId,
                                               Set<String> productItemTypes, String paymentCurrency) {
        final Map<String, UnitAmount> taxByItemSedId = new HashMap<>();
        double totalTax = 0.0;
        Set<String> taxTypes = Set.of("ItemSalesTax", "ItemVatTax");
        List<EntityValue> taxItems = ef.find("mantle.order.OrderItem")
                .condition("orderId",orderId)
                .condition("orderPartSeqId", orderPartSeqId)
                .condition("itemTypeEnumId", EntityCondition.ComparisonOperator.IN, taxTypes)
                .condition("parentItemSeqId", EntityCondition.ComparisonOperator.IS_NOT_NULL)
                .forUpdate(false).useCache(true).list();
        if(!taxItems.isEmpty()) {
            totalTax = taxItems.stream().map((taxEntity) -> {
                String parentItemSeqId = (String) taxEntity.getNoCheckSimple("parentItemSeqId");
                double taxAmount = EntityUtil.getItemTotalPrice(taxEntity)[0];
                UnitAmount tax = taxByItemSedId.computeIfAbsent(parentItemSeqId, k->new UnitAmount());
                tax.setCurrencyCode(paymentCurrency);
                tax.setValue("" + taxAmount);
                return taxAmount;
            }).reduce(0.0, Double::sum);
        } else {
            totalTax = -1;
        }

        List<EntityValue> productItems = ef.find("mantle.order.OrderItem")
                .condition("orderId", orderId)
                .condition("orderPartSeqId", orderPartSeqId)
//                .condition("itemTypeEnumId", EntityCondition.ComparisonOperator.IN, productItemTypes)
//                .condition("parentItemSeqId", EntityCondition.ComparisonOperator.IS_NULL)
                .forUpdate(false).useCache(true).list();
        LOGGER.debug(String.format("Number of order items in orderId '%s', seqID '%s' is %d ", orderId, orderPartSeqId, productItems.size()));
        double totalProductAmount = productItems.isEmpty() ? -1 : 0.0;
        for(EntityValue item : productItems) {
            Map<String, Object> itemInfo = item.getEtlValues();
            double[] costArr = EntityUtil.getItemTotalPrice(item);
            String productId = (String) itemInfo.get("productId");
            EntityValue product = ef.fastFindOne("mantle.product.Product", true, true, productId);
            String pseudoId = (String) product.getNoCheckSimple("pseudoId");

            PurchaseItem purItem = new PurchaseItem();
            purItem.setName((String) product.getNoCheckSimple("productName"));
            purItem.setDescription((String) itemInfo.get("itemDescription"));
            purItem.setQuantity("" + ((int) costArr[1]));
            purItem.setUnitAmount(new UnitAmount("" + costArr[2], paymentCurrency));
            purItem.setUrl(EntityUtil.getFoProductUrl(productStore, pseudoId));
            purItem.setImageUrl(EntityUtil.getFoProductImageUrl(productStore, pseudoId));

            purItem.setTax(taxByItemSedId.get((String) itemInfo.get("orderItemSeqId")));
            purchaseItems.add(purItem);
            totalProductAmount = totalProductAmount + costArr[0];
        }
        return new double[]{totalProductAmount, totalTax};
    }
    private static String getGeoCode(EntityFacade ef, Object geoId, String column) {
        if(geoId == null) return null;
        EntityValue geo = ef.fastFindOne("moqui.basic.Geo", true, true, geoId);
        if(geo == null) return null;
        return (String) geo.getNoCheckSimple(column);
    }
    private static Shipping buildShipping(EntityFacade ef, EntityValue orderPart) {
        Shipping shipping = new Shipping();
        shipping.setType(Shipping.Type.SHIPPING);

        Object postalContactMechId = orderPart.getNoCheckSimple("postalContactMechId");
        EntityValue postalContact = ef.fastFindOne("mantle.party.contact.PostalAddress",
                true, true, postalContactMechId);
        Map<String, Object> postalMap = postalContact.getEtlValues();
        shipping.setName((String) postalMap.get("toName"));

        Address address = new Address();
        address.setAddressLine1((String) postalMap.get("address1"));
        address.setAddressLine2((String) postalMap.get("address2"));
        address.setCountryCode(getGeoCode(ef, postalMap.get("countryGeoId"), "geoCodeAlpha2"));
        address.setAdminArea1(getGeoCode(ef, postalMap.get("stateProvinceGeoId"), "geoCodeAlpha2"));
        Object oCityGeoId = postalMap.get("cityGeoId");
        String city = oCityGeoId != null ? getGeoCode(ef, oCityGeoId, "geoName") : (String) postalMap.get("city");
        address.setAdminArea2(city);
        address.setPostalCode((String) postalMap.get("postalCode"));
        shipping.setAddress(address);

        Object telecomContactMechId = orderPart.getNoCheckSimple("telecomContactMechId");
        if (telecomContactMechId == null) telecomContactMechId = postalMap.get("telecomContactMechId");
        if(telecomContactMechId != null) {
            EntityValue telecomContact = ef.fastFindOne("mantle.party.contact.TelecomNumber",
                    true, true, telecomContactMechId);
            Map<String, Object> telecomMap = telecomContact.getEtlValues();
            String countryCode = (String) telecomMap.get("countryCode");
            String contactNum = (String) telecomMap.get("contactNumber");
            if(countryCode != null && contactNum != null) {
                String areaCode = (String) telecomMap.get("areaCode");
                String telephone = (areaCode == null ? "" : areaCode.trim()) + contactNum.trim();
                PhoneNumber phone = new PhoneNumber();
                phone.setCountryCode(countryCode);
                phone.setNationalNumber(telephone);
                shipping.setPhone(phone);
            }
        }

        return shipping;
    }
    private static PurchaseUnit buidPurchaseUnit(EntityFacade ef,  EntityValue orderPart, Map<String, Object> productStore,
                                                 String paymentCurrency, Set<String> productItemTypes) {
        final String orderId = (String) orderPart.getNoCheckSimple("orderId");
        final String orderPartSeqId = (String) orderPart.getNoCheckSimple("orderPartSeqId");

        AmountBreakDown breakDown = new AmountBreakDown();
        double shippingTotal = getItemsTotalByType(ef, orderId, orderPartSeqId, "ItemShipping");
        if(shippingTotal >= 0) {
            breakDown.setShipping(new UnitAmount("" + shippingTotal, paymentCurrency));
        }
        double discount = getItemsTotalByType(ef, orderId, orderPartSeqId, "ItemDiscount");
        if(discount >= 0) {
            breakDown.setDiscount(new UnitAmount("" + discount, paymentCurrency));
        }

        List<PurchaseItem> purchaseItems = new ArrayList<>();
        double[] totals = buildPurchaseItems(purchaseItems, ef,
                productStore, orderId, orderPartSeqId, productItemTypes, paymentCurrency);
        if(totals[0] >= 0) {
            breakDown.setItemTotal(new UnitAmount("" + totals[0], paymentCurrency));
        }
        if(totals[1] >= 0) {
            breakDown.setTaxTotal(new UnitAmount("" + totals[1], paymentCurrency));
        }

        BigDecimal oPartTotal = (BigDecimal) orderPart.getNoCheckSimple("partTotal");
        TotalAmount totalAmount = new TotalAmount(oPartTotal != null ? oPartTotal.toString() : "0.0", paymentCurrency);
        if(!breakDown.isEmpty()) {
            totalAmount.setBreakDown(breakDown);
        }

        PurchaseUnit p = new PurchaseUnit();
        p.setRefId(orderPart.getNoCheckSimple("orderId") + "_" + orderPart.getNoCheckSimple("orderPartSeqId"));
        p.setShipping(buildShipping(ef, orderPart));
        p.setAmount(totalAmount);
        if(!purchaseItems.isEmpty()) {
            p.setItems(purchaseItems);
        }
        p.setSoftDescriptor((String) productStore.get("storeName"));
        return p;
    }
    private static String getPPRequestId(String paymentId, Operation operation) {
        return paymentId + "_" + operation.ordinal();
    }

    //    private static KeyVal getSavedResponses(String paymentId, Operation operation, EntityFacade ef) {
//        String idStr = paymentId + "_" + operation.ordinal();
//        if(ef == null) {
//            return KV(idStr, null);
//        }
//        EntityList responses = ef.find("mantle.account.method.PaymentGatewayResponse")
//                .condition("paymentGatewayResponseId", EntityCondition.ComparisonOperator.LIKE, idStr + "_%")
//                .selectField("paymentGatewayResponseId").orderBy("-paymentGatewayResponseId")
//                .list();
//        if(responses == null || responses.isEmpty()) {
//            return KV(idStr + "_0", null);
//        }
//
//        for(EntityValue response : responses) {
//            String id = (String) response.getNoCheckSimple("paymentGatewayResponseId");
//            String[] tokens = id.split("_");
//            if (tokens.length < 3) {
//                idStr = idStr + "_0";
//                break;
//            }
//            try {
//                int seq = Integer.parseInt(tokens[2]) + 1;
//                idStr = idStr + "_" + seq;
//                break;
//            } catch (NumberFormatException e) {
//                LOGGER.warn("PaymentGatewayResponse record ID '" + id + "' is not in a valid format");
//            }
//        }
//        return KV(idStr, responses.getFirst());
//    }
    private static EntityValue newResponseObj(EntityFacade ef, EntityValue payment, Operation operation,
                                              PayPalResponse response) {
        EntityValue responseObj = ef.makeValue("mantle.account.method.PaymentGatewayResponse");
        responseObj.setSequencedIdPrimary();
        responseObj.put("transactionDate", new Timestamp(System.currentTimeMillis()));
        responseObj.put("paymentId", payment.getNoCheckSimple("paymentId"));
        responseObj.put("paymentMethodId", payment.getNoCheckSimple("paymentMethodId"));
        responseObj.put("amount", payment.getNoCheckSimple("amount"));
        responseObj.put("amountUomId", payment.getNoCheckSimple("amountUomId"));
        responseObj.put("referenceNum", response.getOrderId());
        responseObj.put("responseCode", response.statusCode);
        responseObj.put("resultSuccess", response.success ? 'Y' : 'N');//
        responseObj.put("resultError", response.success ? 'N' : 'Y');
        responseObj.put("resultDeclined", response.success ? 'N' : 'Y');
        responseObj.put("paymentOperationEnumId", operation.id);
        responseObj.put("reasonMessage", response.getMessage());

        responseObj.put("approvalCode", "");
        responseObj.put("reasonCode", "");
        responseObj.put("avsResult", "");
        responseObj.put("cvResult", "");
        responseObj.put("resultBadExpire", "");
        responseObj.put("resultBadCardNumber", "");
        responseObj.put("resultNsf", "");
        return responseObj;
    }
    private static XContextPayPal newXContext() {
        XContextPayPal xContext = new XContextPayPal();
        //xContext.setShippingPref(XContextBase.ShippingPref.SET_PROVIDED_ADDRESS);
        xContext.setShippingPref(XContextBase.ShippingPref.NO_SHIPPING);
        xContext.setLandingPage(XContextBase.LandingPage.LOGIN);
        xContext.setPaymentMethodPref(XContextBase.PaymentMethodPref.IMMEDIATE_PAYMENT_REQUIRED);
        xContext.setUserAction(XContextBase.UserAction.PAY_NOW);
        return xContext;
    }
    private static EntityValue getEntityFromContext(ExecutionContext ec, String... keys) {
        EntityFacade ef = ec.getEntity();
        ContextStack cs = ec.getContext();
        EntityValue entity = (EntityValue) cs.get(keys[0]);
        if(entity == null && keys[1] != null && keys[2] != null) {
            String pk = (String) cs.get(keys[1]);
            if (pk != null) {
                entity = ef.fastFindOne(keys[2], true, true, pk);
            }
        }
        return entity;
    }
    private static Intent getIntentFromContext(ContextStack cs, Intent defaultIntent) {
        String sIntent = (String) cs.get("intent");
        return sIntent == null ? defaultIntent : Intent.valueOf(sIntent);
    }
    private static PaymentSource getPaymentSource(ContextStack cs) {
        PaymentSource paymentSource = null;
        Object oPaymentSource = cs.get("paymentSource");
        //if(oPaymentSource == null) oPaymentSource = cs.get("payment_source");
        if(oPaymentSource != null) {
            if(oPaymentSource instanceof String) {
                String paymentType = ((String) oPaymentSource).trim();
                LOGGER.debug("Payment type = " + paymentType);
                PayerBase.Type type = PayerBase.Type.fromString(paymentType);
                paymentSource = new PaymentSource(type);
            } else if(oPaymentSource instanceof PaymentSource) {
                paymentSource = (PaymentSource) oPaymentSource;
            } else {
                paymentSource = new PaymentSource((Map<String, Object>) oPaymentSource);
            }
        }
        return paymentSource;
    }
    private static XContextPayPal setPayPalContext(PaymentSource src) {
        PayerBase payer = src.getPayer(PayerBase.Type.PAYPAL);
        if(payer == null) {
            return null;
        }
        XContextPayPal xContext = newXContext();
        payer.setExperienceContext(newXContext());
        return xContext;
    }
    private static class KeyVal { final String k; final Object v; KeyVal(String k, Object v) {this.k = k; this.v = v;}}
    private static KeyVal KV(String key, Object value) { return new KeyVal(key, value); }
    private static Map<String, Object> fail(String message, String detail, KeyVal... kvs) {
        StringBuilder sb = new StringBuilder(message);
        Map<String, Object> map = new HashMap<>(kvs.length + 3);
        map.put("success", 'N');
        map.put("message", message);
        if(detail != null) {
            map.put("detail", detail);
            sb.append(Character.LINE_SEPARATOR).append(detail);
        }
        if(kvs.length > 0) {
            sb.append(": ");
            for (KeyVal kv : kvs) {
                map.put(kv.k, kv.v);
                sb.append(Character.LINE_SEPARATOR).append(kv.k).append(": ")
                        .append(ObjectUtilities.toPlainString(kv.v));
            }
        }
        LOGGER.error(sb.toString());
        return map;
    }
    private static Map<String, Object> success(KeyVal... kvs) {
        Map<String, Object> map = new HashMap<>(kvs.length + 1);
        map.put("success", 'Y');
        for (KeyVal kv : kvs) {
            map.put(kv.k, kv.v);
        }
        return map;
    }
    private static EntityValue getPaypalAccount(EntityFacade ef, Object paymentMethodId) {
        return ef.fastFindOne("mantle.account.method.PayPalAccount",
                true, true, paymentMethodId);
    }
    private static String getPaypalAccountId(ExecutionContext ec, PPClientConfig ppClientConfig) {
        String merchantPPAccountId = getPaypalAccountId(ec);
        if(merchantPPAccountId != null) return merchantPPAccountId;
        if(ppClientConfig != null) {
            LOGGER.warn("Paypal account of merchant is not found or have no payerId"
                    + ". Using merchant ID from PayPal app config");
            return ppClientConfig.getAccountId();
        }
        return null;
    }
    private static String getPaypalAccountId(ExecutionContext ec) {
        ContextStack cs = ec.getContext();
        String merchantPPAccountId = (String) cs.get("paypalMerchantId");
        if(merchantPPAccountId == null) {
            EntityValue paypalAccount = getEntityFromContext(ec,
                    "merchantPayPalAccount", "toPaymentMethodId", "mantle.account.method.PayPalAccount");
            if(paypalAccount != null) {
                merchantPPAccountId = (String) paypalAccount.getNoCheckSimple("payerId");
            }
        }
        return merchantPPAccountId;
    }
//    protected String createPurchaseUnit(EntityValue orderPart) {
//        PurchaseUnit p = new PurchaseUnit();
//        orderPart
//    }
    public static Map<String, Object> createOrder(ExecutionContext ec) {
        EntityFacade ef = ec.getEntity();
        ContextStack cs = ec.getContext();
        Operation op = Operation.CREATE_ORDER;

        String paymentId = (String) cs.get("paymentId");
        EntityValue payment = ef.find("mantle.account.payment.Payment")
                .condition("paymentId", paymentId).forUpdate(true).one();

        MandatoryFieldError mandatoryError = MandatoryFieldError.check(payment,
                MandatoryFieldError.DEF_ERR_MSG_PATTERN + " in payment id '" + paymentId +"'",
                "statusId", "paymentGatewayConfigId");
        if(mandatoryError != null) {
            return mandatoryError;
        }
        String statusId = (String) payment.getNoCheckSimple("statusId");
        if(statusId == null) statusId = "PmntProposed";
        if(statusId.equals("PmntPromised")) {
            Object paymentRefNum = payment.getNoCheckSimple("paymentRefNum");
            //TODO: if paymentRefNum is null, query PaymentGatewayResponse and update payment.
            //TODO: if no PaymentGatewayResponse, request PayPal again with the same requestID as previous request
            return Map.of( "success", 'Y',
                    "paypalOrderId", paymentRefNum
            );
        }
        if(!op.fromStatuses.contains(statusId)) {
            return Map.of("success", 'N',
                    "message", String.format("Payment's status  is inappropriate for an %s operation", op.name()),
                    "paymentStatus", statusId
            );
        }
        //KeyVal kv = getSavedResponses(paymentId, op, ef);
//        EntityValue responseObj = (EntityValue) kv.v;
//        if(responseObj != null && "Y".equals(responseObj.getNoCheckSimple("resultSuccess"))) {
//            LOGGER.warn();
//            return Map.of("success", 'Y'
//                    , "paypalOrderId", responseObj.getString("referenceNum")
//                    , "paymentGatewayResponseId", responseId
//                    , "paymentStatus","PmntPromised");
//        }

        String paymentGatewayConfigId = (String) payment.getNoCheckSimple("paymentGatewayConfigId");

        PPClientConfig ppClientCfg = ec.getTool("PayPalClientConfig", PPClientConfig.class,
                paymentGatewayConfigId);
        if(ppClientCfg == null) {
            String detail = "Cannot retrieve PayPal Client tool instance for paymentGatewayConfigId '" + paymentGatewayConfigId +"' ";
            return fail(PAYPAL_ERR_MSG, detail, KV("paymentGatewayConfigId", paymentGatewayConfigId));
        }

        String merchantPPAccountId = getPaypalAccountId(ec, ppClientCfg);
        if(merchantPPAccountId == null) {
            return fail(PAYPAL_ERR_MSG, "Cannot find PayPal account of merchant");
        }

        String merchantEmail = null;
        if(merchantPPAccountId.contains("@")) {
            merchantEmail = merchantPPAccountId;
            merchantPPAccountId = null;
        }

        EntityValue storeEntity = getEntityFromContext(ec, "productStore",
                "productStoreId", "mantle.product.store.ProductStore");
        if(storeEntity == null) {
            return fail(PAYPAL_ERR_MSG, "Either productStore or productStoreId must exists in context");
        }
        Map<String, Object> productStore = storeEntity.getEtlValues();

        OrderCreateClient client = ppClientCfg.newOrderCreateClient();
        String ppReqId = getPPRequestId(paymentId, op);
        client.withHeader(Http.Header.PAYPAL_REQ_ID, ppReqId);

        Object shouldAuthAssert = cs.get("withAuthAssertion");
        if(shouldAuthAssert != null && shouldAuthAssert.equals('Y')) {
            client.setAuthAssertionJwt(merchantPPAccountId);
        }

        EntityValue paymentUom = payment.findRelatedOne("amountUom", true, false);
        String paymentCurrency = paymentUom != null ? (String) paymentUom.getNoCheckSimple("abbreviation") : "USD";

        Set<String> productItemTypes =  EntityUtil.getOrderItemProductTypes(ef);
        LOGGER.debug("Product item types: " + String.join(", ", productItemTypes));
        List<EntityValue> orderParts = (List<EntityValue>) cs.get("orderParts");
        if(orderParts == null) {
            EntityValue orderPart = (EntityValue) cs.get("orderPart");
            orderParts = List.of(orderPart);
        }
        for (EntityValue orderPart : orderParts) {
            PurchaseUnit p = buidPurchaseUnit(ef, orderPart, productStore, paymentCurrency, productItemTypes);
            p.setPayee(new Merchant(merchantPPAccountId, merchantEmail));
            client.addPurchaseUnit(p);
        }

        client.setIntent(getIntentFromContext(cs, Intent.AUTHORIZE));
        PaymentSource pSrc = getPaymentSource(cs);
        if(pSrc == null) {
            return fail("payment_source is missing", null);
        }
        setPayPalContext(pSrc);
        client.setPaymentSource(pSrc);

        client.call();
        PayPalResponse response = client.getResponseData();
        EntityValue responseObj = newResponseObj(ef, payment, op, response);
        responseObj.put("paymentGatewayConfigId", paymentGatewayConfigId);
        responseObj.put("altReference", ppReqId);
        responseObj.create();
        String responseId = (String) responseObj.getNoCheckSimple("paymentGatewayResponseId");

        Map<String, Object> result;
        if(response.success) {
            payment.set("statusId", "PmntPromised");
            payment.set("paymentRefNum", response.getOrderId());
            payment.update();
            result = Map.of("success", 'Y'
                    , "paypalOrderId", response.getOrderId()
                    , "paymentGatewayResponseId", responseId
                    , "paymentStatus","PmntPromised"
            );
        } else {
            String errMsg = "Failed to create PayPal order: " + response.getMessage();
            LOGGER.error(errMsg);
            List<ReqFieldError> fieldErrors = response.getFieldErrors();
            if(fieldErrors != null) {
                LOGGER.error("Details: ");
                for(ReqFieldError fieldError : fieldErrors) {
                    LOGGER.error(String.format("Field '%s' with value '%s' in %s: %s",
                            fieldError.getNamePath(), fieldError.getValue(),
                            fieldError.getLocation(), fieldError.getIssue()));
                }
            }
            result = Map.of("success", 'N',
                    "message", errMsg,
                    "fields", fieldErrors == null ? List.of() : fieldErrors,
                    "paymentGatewayResponseId", responseId
            );
        }

        return result;
    }

    public static Map<String, Object> submitPayment(ExecutionContext ec) {
        EntityFacade ef = ec.getEntity();
        ContextStack cs = ec.getContext();

        String sOp = (String) cs.get("operation");
        Operation op = sOp != null ? Operation.fromString(sOp) : null;
        if(op == null) {
            String errMsg = "Paypal operation is missing or invalid";
            return fail(errMsg, errMsg + ": " + sOp);
        }

        String paymentId = (String) cs.get("paymentId");
        EntityValue payment = ef.fastFindOne("mantle.account.payment.Payment",
                        true, true, paymentId);
        MandatoryFieldError mandatoryError = MandatoryFieldError.check(payment,
                MandatoryFieldError.DEF_ERR_MSG_PATTERN + " in payment id '" + paymentId +"'",
                "paymentRefNum", "statusId");
        if(mandatoryError != null) {
            return mandatoryError;
        }
        String statusId = (String) payment.getNoCheckSimple("statusId");
        if(statusId == null) statusId = "PmntProposed";
        if(op == Operation.AUTHORIZE && "PmntAuthorized".equals(statusId)) {
            op = Operation.REAUTHORIZE;
        } else if(op == Operation.REAUTHORIZE && "PmntPromised".equals(statusId)) {
            op = Operation.AUTHORIZE;
        }
        if(!op.fromStatuses.contains(statusId)) {
            String errMsg = String.format("Payment's status  is inappropriate for an %s operation", op.name());
            LOGGER.error(errMsg);
            return Map.of("success", 'N',
                    "message", errMsg,
                    "paymentStatus", statusId
            );
        }

        String paypalOrderId = (String) payment.getNoCheckSimple("paymentRefNum");
        String paymentGatewayConfigId = (String) payment.getNoCheckSimple("paymentGatewayConfigId");
        if(paymentGatewayConfigId == null) {
            paymentGatewayConfigId = (String) cs.get("paymentGatewayConfigId");
        }
        PPClientConfig ppClientCfg = ec.getTool("PayPalClientConfig", PPClientConfig.class,
                paymentGatewayConfigId);
        if(ppClientCfg == null) {
            String detail = "Cannot retrieve PayPal Client tool instance for paymentGatewayConfigId '" + paymentGatewayConfigId +"' ";
            return fail(PAYPAL_ERR_MSG, detail, KV("paymentGatewayConfigId", paymentGatewayConfigId));
        }

        AuthorizeClient client = ppClientCfg.newAuthCaptureClient(paypalOrderId, op);
        Object shouldAuthAssert = cs.get("withAuthAssertion");
        if(shouldAuthAssert != null && shouldAuthAssert.equals('Y')) {
            EntityValue paypalAccount = getPaypalAccount(ef, payment.getNoCheckSimple("toPaymentMethodId"));
            if(paypalAccount == null) {
                return fail(PAYPAL_ERR_MSG, "Cannot find PayPal account of merchant");
            }
            client.setAuthAssertionJwt((String) paypalAccount.getNoCheckSimple("payerId"));
        }
        BigDecimal amount = (BigDecimal) cs.get("amount");
        if(amount != null) {
            EntityValue paymentUom = payment.findRelatedOne("amountUom", true, false);
            String currency = paymentUom != null ? (String) paymentUom.getNoCheckSimple("abbreviation") : "USD";
            client.setAmount(amount.doubleValue(), currency);
        }
        PaymentSource pSrc = getPaymentSource(cs);
        if(pSrc != null) {
            //TODO: check necessary and set payer info
            setPayPalContext(pSrc);
            client.setPaymentSource(pSrc);
        }

        //client.withHeader(Http.Header.PAYPAL_REQ_ID, responseId);
        client.call();
        PayPalResponse response = client.getResponseData();
        EntityValue responseObj = newResponseObj(ef, payment, op, response);
        responseObj.put("paymentGatewayConfigId", paymentGatewayConfigId);
        responseObj.create();
        String responseId = (String) responseObj.getNoCheckSimple("paymentGatewayResponseId");

        Map<String, Object> result;
        if(response.success) {
            result = Map.of("success", 'Y'
                    , "paypalOrderId", response.getOrderId()
                    , "paymentGatewayResponseId", responseId
                    , "paymentStatus","PmntAuthorized"
            );
        } else {
            String errMsg = String.format("Failed to %s PayPal order: %s", op.name(), response.getMessage());
            LOGGER.error(errMsg);
            result = Map.of("success", 'N',
                    "message", errMsg,
                    "paymentGatewayResponseId", responseId
            );
        }
        return result;
    }
}
