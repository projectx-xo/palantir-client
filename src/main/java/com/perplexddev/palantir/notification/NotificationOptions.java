package com.perplexddev.palantir.notification;

/**
 * Immutable notification settings derived from the configuration.
 *
 * <p>Colours are resolved to ARGB ints and volume/scale to normalised floats, so nothing is parsed
 * or converted per frame.
 */
public record NotificationOptions(boolean enabled,
                                  NotificationCorner corner,
                                  int offsetX,
                                  int offsetY,
                                  long durationMs,
                                  long fadeInMs,
                                  long fadeOutMs,
                                  boolean playSound,
                                  float soundVolume,
                                  float scale,
                                  int maxSimultaneous,
                                  int backgroundColor,
                                  int borderColor,
                                  int titleColor,
                                  int bodyColor,
                                  boolean roundedCorners) {
}
