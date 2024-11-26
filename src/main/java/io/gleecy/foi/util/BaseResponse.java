package io.gleecy.foi.util;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BaseResponse extends DTOBase {
    public BaseResponse() {
        super();
    }

    public BaseResponse(Map<? extends String, ?> data) {
        super(data);
    }
    @Override
    public @Nullable Object put(String key, Object value) {
        if("success".equals(key)) {
            if(value == null) {
                return setSuccess(false);
            }
            if(value instanceof Character) {
                return setSuccess('Y' == (Character) value);
            }
            if(value instanceof CharSequence) {
                return setSuccess((String) value);
            }
            return setSuccess((Boolean) value);
        }
        return super.put(key, value);
    }
    public Character setSuccess(String success) {
        if(success == null) {
            return setSuccess(false);
        }

        return setSuccess("Y".equalsIgnoreCase(success.trim()));
    }
    public Character setSuccess(boolean isSuccessful) {
        return (Character) this._map.put("success", isSuccessful ? 'Y' : 'N');
    }
    public Character getSuccess() {
        return (Character) this.get("success");
    }
    public boolean isSuccess() {
        Character oC = this.getSuccess();
        return oC != null && oC == 'Y';
    }
    public String getMessage() {
        return (String) this._map.get("message");
    }
    public String setMessage(String message) {
        return (String) this._map.put("message", message);
    }
}
