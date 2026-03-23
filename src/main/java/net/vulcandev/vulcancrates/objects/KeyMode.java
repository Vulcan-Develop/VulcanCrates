package net.vulcandev.vulcancrates.objects;

import java.util.Locale;

/**
 * Supported key delivery and usage modes for the plugin.
 */
public enum KeyMode {
    VIRTUAL,
    PHYSICAL;

    public static KeyMode fromConfig(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return VIRTUAL;
        }

        try {
            return KeyMode.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return VIRTUAL;
        }
    }

    public String getDisplayName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
