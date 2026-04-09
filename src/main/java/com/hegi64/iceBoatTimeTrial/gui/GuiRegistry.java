package com.hegi64.iceBoatTimeTrial.gui;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GuiRegistry {
    private final Map<String, GuiMenu> byId = new ConcurrentHashMap<>();

    public void register(GuiMenu menu) {
        byId.put(menu.id(), menu);
    }

    public Optional<GuiMenu> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}

