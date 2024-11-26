package io.gleecy.foi.util;

import groovy.json.JsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class DTOBase implements DTO, Map<String, Object> {
    //private static final Logger LOGGER = LoggerFactory.getLogger(DTO.class);
    private final Class<? extends DTOBase> thisClass = this.getClass();
    protected final Map<String, Object> _map = new HashMap<>();
    public DTOBase() {}
    public DTOBase(Map<? extends String, ?> data) {
        this.putAll(data);
    }
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() { return null; }
    protected Map<String, Function<String, ? extends DTO>> getStringConverters() { return null; }
    public String toJson() {
        JsonBuilder jb = new JsonBuilder();
        return jb.call(_map).toString();
    }
    @Override
    public int size() {
        return _map.size();
    }

    @Override
    public boolean isEmpty() {
        return _map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return _map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return _map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return _map.get(key);
    }

    protected Function<String, ? extends DTO> getStrConverter(String key) {
        Map<String, Function<String, ? extends DTO>> converterMap =
                getStringConverters();
        if(converterMap == null) return null;
        return converterMap.get(key);
    }
    protected Function<Map<String, Object>, ? extends DTOBase> getMapConverter(String key) {
        Map<String, Function<Map<String, Object>, ? extends DTOBase>> converterMap =
                getMapConverters();
        if(converterMap == null) return null;
        return converterMap.get(key);
    }
    protected Object doConvert(String key, Object value) {
        if(value instanceof Map) {
            Function<Map<String, Object>, ? extends DTOBase> converter = getMapConverter(key);
            return converter == null ? value : converter.apply((Map<String, Object>) value);
        }
        if(value instanceof String) {
            Function<String, ? extends DTO> converter = getStrConverter(key);
            return converter == null ? value : converter.apply((String) value);
        }
        if(value instanceof List) {
            List<Object> srcVals = (List<Object>) value;
            ArrayList<Object> results = new ArrayList<>(srcVals.size());
            srcVals.forEach((item) -> {
                results.add(doConvert(key, item));
            });
            return results;
        }
        return value;
    }

    @Nullable
    @Override
    public Object put(String key, Object value) {
        if(value == null) return _map.get(key);
        if (value instanceof DTO) return _map.put(key, value);
        return _map.put(key, doConvert(key, value));
//            throw new RuntimeException(String.format(
//                    "Failed to convert field '%s' of class '%s'. Error: %s",
//                    key, thisClass.getName(), e.getMessage()), e);
    }

    @Override
    public Object remove(Object key) {
        return _map.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ?> m) {
        Set<? extends Entry<? extends String, ?>> entries = m.entrySet();
        for(Entry<? extends String, ?> entry : entries) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        _map.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return _map.keySet();
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        return _map.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return _map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        if(thisClass != o.getClass()) {
            return false;
        }
        return _map.equals(((DTOBase) o)._map);
    }

    @Override
    public int hashCode() {
        return _map.hashCode();
    }
}
