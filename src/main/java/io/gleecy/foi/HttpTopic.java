package io.gleecy.foi;

public interface HttpTopic {
    void addParam(String key, String value);
    void send();
}
