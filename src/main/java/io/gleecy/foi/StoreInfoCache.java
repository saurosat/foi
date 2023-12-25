package io.gleecy.foi;

import org.moqui.context.ExecutionContextFactory;
import org.moqui.context.ToolFactory;
import org.moqui.entity.EntityCondition;
import org.moqui.entity.EntityFind;
import org.moqui.entity.EntityList;
import org.moqui.entity.EntityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class StoreInfoCache implements ToolFactory<StoreInfo> {
    private static final Logger logger = LoggerFactory.getLogger(StoreInfoCache.class);
    public final Map<String, StoreInfo> storeById = new HashMap<>();
    public final Map<String, LinkedList<StoreInfo>> storesByTenant = new HashMap<>();
    /**
     * Return a name that the factory will be available under through the ExecutionContextFactory.getToolFactory()
     * method and instances will be available under through the ExecutionContextFactory.getTool() method.
     */
    @Override
    public String getName() {
        return "StoreInfo";
    }

    /**
     * Initialize the underlying tool and if the instance is a singleton also the instance.
     *
     * @param ecf
     */
    @Override
    public void init(ExecutionContextFactory ecf) {
        EntityFind find = ecf.getEntity().find(StoreInfo.ENAME);
        find.selectField("productStoreId")
                .selectField("organizationPartyId")
                .selectField("secretKey")
                .selectField("notificationUrl")
                .selectField("subscribedEntities");
        find.condition("secretKey", EntityCondition.IS_NOT_NULL, null);
        find.forUpdate(false);
        find.useCache(true);
        find.disableAuthz();
        EntityList eList = find.list();
        for(EntityValue entity : eList) {
            StoreInfo store = StoreInfo.newInstance(entity);
            if(store != null ){
                this._addStore(entity);
            }
        }
    }
    synchronized StoreInfo _removeStore(String storeId) {
        StoreInfo store = this.storeById.remove(storeId);
        if(store != null) {
            LinkedList<StoreInfo> stores = this.storesByTenant.get(store.tenantPrefix);
            stores.remove(store);
        }
        return store;
    }
    synchronized StoreInfo _addStore(EntityValue entity) {
        return _addStore(StoreInfo.newInstance(entity));
    }
    synchronized StoreInfo _addStore(StoreInfo store) {
        if(store == null) return null;
        LinkedList<StoreInfo> tenantStores = this.storesByTenant.computeIfAbsent(
                store.tenantPrefix, k -> new LinkedList<>());
        tenantStores.add(store);
        this.storeById.put(store.storeId, store);
        store.storeCache = this;
        logger.info("Created store {ID: " + store.storeId +", prefix: " + store.tenantPrefix + "}");
        return store;
    }

    /**
     * Called by ExecutionContextFactory.getTool() to get an instance object for this tool.
     * May be created for each call or a singleton.
     *
     * @param parameters
     * @throws IllegalStateException if not initialized
     */
    @Override
    public StoreInfo getInstance(Object... parameters) {
        if(parameters == null || parameters.length == 0
                || parameters[0] == null) {
            logger.error(this.getName() + " invoked with empty parameters");
            return null;
        }
        String storeId = null;
        if(parameters[0] instanceof String) {
            storeId = (String) parameters[0];
        } else if(parameters[0] instanceof EntityValue) {
            EntityValue entity = (EntityValue) parameters[0];
            if(!StoreInfo.ENAME.equals(entity.getEntityName())) {
                return null;
            }
            storeId = (String) entity.getNoCheckSimple("productStoreId");
        }
        if(storeId == null || storeId.isEmpty()) {
            return null;
        }
        StoreInfo store = storeById.get(storeId);
        if(store == null) {
            store = new StoreInfo(storeId);
            store.storeCache = this;
        }
        return store;
    }
}
