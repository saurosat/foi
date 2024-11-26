package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

public class ReqFieldError extends DTOBase {
    public ReqFieldError() {
        super();
    }

    public ReqFieldError(Map<? extends String, ?> data) {
        super(data);
    }
    public String getNamePath() {
        return (String) this.get("field");
    }
    public String getValue() {
        return (String) this.get("value");
    }

    public String getLocation() {
        return (String) this.get("location");
    }
    public String getIssue() {
        return (String) this.get("issue");
    }
    public String getDescription() {
        return (String) this.get("description");
    }
}
