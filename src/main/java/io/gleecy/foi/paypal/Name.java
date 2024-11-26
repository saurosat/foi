package io.gleecy.foi.paypal;

import io.gleecy.foi.util.DTO;
import io.gleecy.foi.util.DTOBase;

import java.util.Map;
import java.util.function.Function;

public class Name extends DTOBase {
    public Name(Map<String, Object> name) {
        super(name);
    }

    /**
     *
     * string <= 140 characters
     * When the party is a person, the party's given, or first, name.
     * @param givenName
     * @return
     */
    public void setGivenName(String givenName) { this.put("given_name", givenName);
    }
    public String getGivenName() {
        return (String) this.get("given_name");
    }

    /**
     * string <= 140 characters
     * When the party is a person, the party's surname or family name.
     * Also known as the last name. Required when the party is a person.
     * Use also to store multiple surnames including the matronymic, or mother's, surname.
     * @param surname
     * @return
     */
    public void setSurname(String surname) { this.put("surname", surname);
    }
    public String getSurname() {
        return (String) this.get("surname");
    }

    @Override
    protected Map<String, Function<Map<String, Object>, ? extends DTOBase>> getMapConverters() {
        return null;
    }

    @Override
    protected Map<String, Function<String, ? extends DTO>> getStringConverters() {
        return null;
    }
}
