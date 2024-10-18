package io.gleecy.foi.service;

import io.gleecy.foi.HttpTopic;
import io.gleecy.foi.StoreInfo;
import io.gleecy.foi.StoreInfoCache;
import org.moqui.context.ExecutionContext;
import org.moqui.context.ToolFactory;
import org.moqui.entity.EntityFind;
import org.moqui.entity.EntityList;
import org.moqui.entity.EntityValue;
import org.moqui.impl.entity.EntityDefinition;
import org.moqui.impl.entity.EntityFacadeImpl;
import org.moqui.impl.entity.EntityValueBase;
import org.moqui.util.ContextStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PublishServices {
    private final static Logger logger = LoggerFactory.getLogger(PublishServices.class);
    private static final String[] virtualProdRelates = new String[] {
            "mantle.product.category.ProductCategoryMember",
            "mantle.product.ProductCalculatedInfo",
            "mantle.product.ProductGeo",
            "mantle.product.ProductIdentification",
            "mantle.product.feature.ProductFeatureAppl",
            "mantle.product.ProductPrice"
    };
    private static final String[] assetProdRelates = new String[] {
            "mantle.product.feature.ProductFeatureAppl",
            "mantle.product.ProductCalculatedInfo",
            "mantle.product.ProductPrice"
    };
    private static final AtomicInteger threadCount = new AtomicInteger(0);
    public static Map<String, Object> publish(ExecutionContext ec) {
        final ContextStack cs = ec.getContext();
        EntityValueBase entity = (EntityValueBase) cs.get("entityValue");
        if(entity == null) {
            logger.error("entityValue is not found in context");
            return Map.of("published", false, "error", "entityValue is not found in context");
        }

        boolean published = false;
        threadCount.incrementAndGet();
        MarkSet markSet = MarkSet.fromEntityName(entity.getEntityDefinition().getEntityName());
        String on = (String) cs.get("eecaOperation");
        if(on != null && on.equalsIgnoreCase("create")) {
            switch (markSet) {
                case ProductCategoryMember:
                    published = publishCategoryMember(ec);
                    break;
                case ProductAssoc:
                    published = publishAssoc(ec);
                    break;
                case Unknown:
                default:
                    published = publishEntity(ec, markSet);
                    break;
            }
        } else {
            published = publishEntity(ec, markSet);
        }
        if(threadCount.decrementAndGet() == 0) {
            MarkSet.clearAll();
        }
        return Map.of("published", published);
    }
    private static boolean publishEntity(ExecutionContext ec, MarkSet markSet) {
        final ContextStack cs = ec.getContext();
        EntityValueBase entity = (EntityValueBase) cs.get("entityValue");
        if(entity == null) {
            logger.error("entityValue is not found in context");
            return false;
        }
        if(!markSet.mark(entity.getPrimaryKeysString())) {
            logger.error("entityValue " + entity.getEntityName() + "." +
                    entity.getPrimaryKeysString() + " is already processed");
            return false;
        }
        String on = (String) cs.get("eecaOperation");
        String storeId = (String) cs.get("productStoreId");
        String categoryId = (String) cs.get("productCategoryId");
        ToolFactory<HttpTopic> toolFactory = ec.getFactory().getToolFactory("HttpTopic");
        HttpTopic topic = toolFactory.getInstance(entity, on, storeId, categoryId);
        if (topic == null) {
            logger.error("Found no subscribers for publishing entity " + entity.getEntityName() );
            return false;
        }
        topic.send();
        return true;
    }
    private static boolean publishCategoryMember(ExecutionContext ec) {
        final ContextStack cs = ec.getContext();
        EntityValueBase entity = (EntityValueBase) cs.get("entityValue");
        if(entity == null) {
            logger.error("object 'entityValue' is not found in context");
            return false;
        }
        String pkStr = entity.getPrimaryKeysString();
        MarkSet markSet = MarkSet.ProductCategoryMember;
        if(markSet.isMarked(pkStr)) {
            return false;
        }

        List<StoreInfo> stores = getAllTenantStores(ec);
        if (stores == null || stores.isEmpty()) {
            return false;
        }
        String memCatId = (String) entity.getNoCheckSimple("productCategoryId");
        boolean isViewAllow = false;
        for(StoreInfo store : stores) {
            if(store.viewableCategoryIds.containsKey(memCatId)) {
                isViewAllow = true;
                break;
            }
        }
        boolean published = false;
        if(isViewAllow) {
            String productId = (String) entity.getNoCheckSimple("productId");
            published = publishRelatedProduct(ec, productId, memCatId);
        }
        if(!published) {
            String on = (String) cs.get("eecaOperation");
            String storeId = (String) cs.get("productStoreId");
            String categoryId = (String) cs.get("productCategoryId");
            ToolFactory<HttpTopic> toolFactory = ec.getFactory().getToolFactory("HttpTopic");
            HttpTopic topic = toolFactory.getInstance(entity, on, storeId, categoryId);
            if (topic == null) {
                return false;
            }
            topic.send();
        }
        return true;
    }
    private static boolean publishRelatedProduct(ExecutionContext ec, String productId, String categoryId) {
        ContextStack cs = ec.getContext();
        EntityFacadeImpl efi = (EntityFacadeImpl) ec.getEntity();
        EntityValue product = efi.fastFindOne("mantle.product.Product",
                true, true, productId);
        ContextStack subCS = cs.push();
        subCS.put("entityValue", product);
        subCS.put("eecaOperation", "create");
        if(categoryId != null) {
            subCS.put("productCategoryId", categoryId);
        }
        boolean published = publishProduct(ec);
        cs.pop();
        return published;
    }

    private static boolean publishProduct(ExecutionContext ec) {
        final ContextStack cs = ec.getContext();
        EntityValue product = (EntityValue) cs.get("entityValue");
        if(product == null) return false;
        if(!MarkSet.Product.mark(product.getPrimaryKeysString())) {
            return false;
        }
        String on = (String) cs.get("eecaOperation");
        String storeId = (String) cs.get("productStoreId");
        String categoryId = (String) cs.get("productCategoryId");
        ToolFactory<HttpTopic> toolFactory = ec.getFactory().getToolFactory("HttpTopic");
        HttpTopic topic = toolFactory.getInstance(product, on, storeId, categoryId);
        if(topic == null) {
            return false;
        }
        topic.send();

        String[] relatedEntityNames = new String[0];
        String productId = (String) product.getNoCheckSimple("productId");
        String productType = (String) product.getNoCheckSimple("productTypeEnumId");
        EntityFacadeImpl efi = (EntityFacadeImpl) ec.getEntity();
        if("PtVirtual".equals(productType)) {
            EntityFind finder = efi.find("mantle.product.ProductAssoc")
                    .disableAuthz().useCache(true).forUpdate(false)
                    .condition("productId", productId)
                    .condition("productAssocTypeEnumId", "PatVariant")
                    .conditionDate(null, null, null);
            EntityList children = finder.list();
            for(EntityValue child : children) {
                ContextStack subCS = cs.push();
                subCS.put("entityValue", child);
                subCS.put("eecaOperation", "create");
                publishAssoc(ec);
                cs.pop();
            }
            relatedEntityNames = virtualProdRelates;
        } else if ("PtAsset".equals(productType)) {
            relatedEntityNames = assetProdRelates;
        }
        for (String entityName : relatedEntityNames) {
            EntityDefinition ed = efi.getEntityDefinition(entityName);
            EntityFind finder = ed.makeEntityFind()
                    .condition("productId", productId)
                    .forUpdate(false).useCache(true).disableAuthz();
            if (ed.isField("thruDate"))
                finder.conditionDate(null, null, null);

            EntityList evs = finder.list();
            for (EntityValue ev : evs) {
                MarkSet evMarkSet = MarkSet.fromEntityName(((EntityValueBase) ev).getEntityDefinition().getEntityName());
                if(evMarkSet.mark(ev.getPrimaryKeysString())) {
                    HttpTopic subTopic = toolFactory.getInstance(ev, "create", storeId, categoryId);
                    if (subTopic != null) {
                        subTopic.send();
                    }
                }
            }
        }
        return true;
    }
    private static boolean publishAssoc(ExecutionContext ec) {
        final ContextStack cs = ec.getContext();
        EntityValueBase assoc = (EntityValueBase) cs.get("entityValue");
        if(assoc == null) {
            logger.error("object 'entityValue' is not found in context");
            return false;
        }
        if(!MarkSet.ProductAssoc.mark(assoc.getPrimaryKeysString())) {
            return false;
        }
        String categoryId = (String) cs.get("productCategoryId");
        String variantId = (String) assoc.getNoCheckSimple("toProductId");
        publishRelatedProduct(ec, variantId, categoryId);

        ToolFactory<HttpTopic> toolFactory = ec.getFactory().getToolFactory("HttpTopic");
        String on = (String) cs.get("eecaOperation");
        String storeId = (String) cs.get("productStoreId");
        HttpTopic topic = toolFactory.getInstance(assoc, on, storeId, categoryId);
        if (topic == null) {
            return false;
        }
        topic.send();
        return true;
    }


    private static List<StoreInfo> getAllTenantStores(ExecutionContext ec) {
        ContextStack cs = ec.getContext();
        String storeId = (String) cs.get("productStoreId");
        if(storeId != null && !storeId.isEmpty()) {
            StoreInfo store = ec.getTool("StoreInfo",
                    StoreInfo.class, storeId);
            if(store != null) {
                return List.of(store);
            }
        }
        StoreInfoCache storeCache = ec.getFactory().getTool("StoreInfo",
                StoreInfo.class, "_NA_").storeCache;
        EntityValueBase entity = (EntityValueBase) cs.get("entityValue");
        String tenantPrefix = entity.getTenantPrefix();
        return storeCache.storesByTenant.get(tenantPrefix);
    }
    private enum MarkSet{
        Product, ProductGeo, ProductPrice, ProductAssoc, ProductIdentification, ProductCalculatedInfo,
        ProductStoreProduct, ProductStoreCategory, ProductStorePromotion, ProductStorePromoProduct,
        ProductFeature, ProductFeatureAppl,
        ProductCategory, ProductCategoryMember, ProductCategoryRollup,
        Unknown;
        private final Set<Object> markSet;
        MarkSet() {
            this.markSet = new HashSet<>();
        }
        private synchronized boolean doMark(Object key) {
            return markSet.add(key);
        }
        public boolean mark(Object key) {
            if(markSet.contains(key)) {
                return false;
            }
            return doMark(key);
        }
        public synchronized boolean isMarked(Object key) {
            return markSet.contains(key);
        }
        private synchronized void doClear() {
            markSet.clear();
        }
        public void clear() {
            if(!markSet.isEmpty()) {
                doClear();
            }
        }
        public static void clearAll() {
            for (MarkSet markSet : MarkSet.values()) {
                markSet.clear();
            }
        }
        public static MarkSet fromEntityName(String shortEntityName) {
            try {
                return MarkSet.valueOf(shortEntityName);
            } catch (IllegalArgumentException e) {
                return MarkSet.Unknown;
            }
        }
    }
}
