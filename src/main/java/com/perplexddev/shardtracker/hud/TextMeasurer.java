package com.perplexddev.shardtracker.hud;

/**
 * Minimal adapter over Minecraft's text renderer, so panel sizing can be built and tested without a
 * running client.
 */
@FunctionalInterface
public interface TextMeasurer {

    int width(String text);
}
