package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTOBase;

import java.util.Map;

public class Hateoas extends DTOBase {
    public Hateoas() {
    }

    public Hateoas(Map<? extends String, ?> data) {
        super(data);
    }

    public String setHref(String href) {
        return (String) this.put("href", href);
    }
    public String getHref() {
        return (String) this.get("href");
    }
    public String setRel(String rel) {
        return (String) this.put("rel", rel);
    }
    public String getRel() {
        return (String) this.get("rel");
    }
}
