package io.gleecy.foi.util;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public class MandatoryFieldError extends BaseResponse {
    public static final String DEF_ERR_MSG_PATTERN = "Required fields %s is missing";
    public static MandatoryFieldError check(Map<String, Object> entity, String errMsgPattern, String... mandatoryKeys) {
        StringBuilder missingKeys = new StringBuilder();
        for(String key : mandatoryKeys) {
            if(entity.get(key) == null) {
                missingKeys.append(key).append(", ");
            }
        }
        int len = missingKeys.length();
        if(len < 3) {
            return null;
        }
        String sKeys = missingKeys.substring(0, len-2);
        if(errMsgPattern == null) {
            errMsgPattern = DEF_ERR_MSG_PATTERN;
        }
        String message = String.format(errMsgPattern, sKeys);
        MandatoryFieldError error = new MandatoryFieldError();
        error.setMessage(message);
        return error;
    }
    private MandatoryFieldError() {
        super();
        setSuccess(false);
    }
    private MandatoryFieldError(Map<? extends String, ?> data) {
        super(data);
        setSuccess(false);
    }
}
