package io.gleecy.foi.tool;

import org.moqui.context.ExecutionContextFactory;
import org.moqui.entity.EntityFacade;
import org.moqui.entity.EntityFind;
import org.moqui.entity.EntityValue;
import org.moqui.impl.entity.EntityDefinition;
import org.moqui.impl.entity.EntityFacadeImpl;
import org.moqui.util.RestClient;

import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public abstract class ClientConfig {
    public final String configId;
    public final RestClient.RequestFactory requestFactory;
    public final ExecutionContextFactory ecf;
    protected String keyBaseUrl = "baseUrl";
    protected String keyAuthToken = "authToken";
    protected String keyClientId = "clientId";
    protected String keySecret = "secret";
    protected String keyBearerToken = "bearerToken";
    protected String keyExpiryTime = "expiryTime";
    protected Map<String, Object> cfgMap = null;
    protected TokenClient tokenClient = null;
    public String error = "ClientConfig instance is not loaded";

    public abstract String getEntityName();
    public TokenClient newTokenClient() {return null; };
    protected ClientConfig(String configId, ExecutionContextFactory ecf, RestClient.RequestFactory requestFactory) {
        this.configId = configId;
        this.ecf = ecf;
        this.requestFactory = requestFactory;
    }

    public boolean isLoaded() { return cfgMap != null; }
    public synchronized void setConfig(Map<String, Object> configMap) {
        this.cfgMap = configMap;
        this.error = checkValidity();
    }
    protected String checkValidity() {
        StringBuilder errMsg = new StringBuilder();
        if(this.getBaseUrl() == null)
            errMsg.append("Gateway base URL is not found from DB").append(", ");
        if(this.keyAuthToken != null && this.getAuthToken() == null)
            errMsg.append("Cannot load either authToken or clientId+sercret from DB").append(", ");
        return errMsg.length() > 0 ? errMsg.toString() : null;
    }
    protected boolean isTokenExpired(Timestamp fromTime) {
        if(this.keyBearerToken == null) return false;
        if(this.getBearerToken() == null) return true;
        if(this.getExpiryTime() == null) return false;
        return this.getExpiryTime().before(fromTime);
    }

    public synchronized void load() {
        EntityFacade ef = ecf.getEntity();
        EntityValue evCfg = ef.fastFindOne(getEntityName(), true, true, configId);
        if(evCfg == null) {
            this.error = "Cannot find " + getEntityName() + " record with ID '" + configId + "' in DB";
            return;
        }
        setConfig(evCfg.getMap());
    }
    public synchronized void save() {
        String entityName = getEntityName();
        EntityFacadeImpl efi = (EntityFacadeImpl) ecf.getEntity();
        EntityFind finder = efi.find(entityName);
        EntityDefinition ed = efi.getEntityDefinition(entityName);
        ed.getPkFieldNames().forEach((fName) -> finder.condition(fName, cfgMap.get(fName)));
        finder.forUpdate(true);
        EntityValue evCfg = finder.one();
        cfgMap.forEach(evCfg::set);
        evCfg.store();
    }
    public String getBaseUrl() {
        return (String) cfgMap.get(keyBaseUrl);
    }
    public String getAuthToken() {
        String authToken = (String) cfgMap.get(keyAuthToken);
        if(authToken == null) {
            String clientId = (String) cfgMap.get(keyClientId);
            String secret = (String) cfgMap.get(keySecret);
            if(clientId == null || secret == null) {
                return null;
            }
            authToken = clientId + ':' + secret;
            authToken = Base64.getEncoder().encodeToString(authToken.getBytes());
            cfgMap.put(keyAuthToken, authToken);
        }
        return authToken;
    }
    public String getBearerToken() {
        return (String) cfgMap.get(keyBearerToken);
    }
    public String setBearerToken(String token) {
        return (String) cfgMap.put(keyBearerToken, token);
    }
    public Timestamp getExpiryTime() {
        return (Timestamp) cfgMap.get(keyExpiryTime);
    }
    public Timestamp setExpiryTime(Timestamp expiryTime) {
        return (Timestamp) cfgMap.put(keyExpiryTime, expiryTime);
    }

    protected TokenClient getTokenClient() {
        if(this.tokenClient == null) {
            this.tokenClient = newTokenClient();
        }
        return this.tokenClient;
    }
    public void reloadToken() {
        Timestamp fromTime = new Timestamp(System.currentTimeMillis());
        if(!isTokenExpired(fromTime)) return;
        synchronized (this) {
            if(isTokenExpired(fromTime)) { //Check again for the case other thread has just updated
                TokenClient client = getTokenClient();
                if(client != null) {
                    client.call();
                    if(client.statusCode < 200 || client.statusCode >= 400) {
                        this.error = "Cannot reload expired token: " + client.getMessage();
                        return;
                    }
                    this.setExpiryTime(client.getExpiryTime());
                    this.setBearerToken(client.getBearerToken());
                }
                this.error = null;
            }
        }
    }

}
