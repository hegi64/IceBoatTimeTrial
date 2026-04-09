package com.hegi64.iceBoatTimeTrial.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GuiContext {
    private final Map<String, Object> values = new HashMap<>();

    public GuiContext put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Context key '" + key + "' is not of type " + type.getSimpleName());
        }
        return (T) value;
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }
}

