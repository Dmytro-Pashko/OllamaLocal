package com.dpashko.localaiclient.domain.models.connection

/**
 * Conservative support state for provider capabilities that may not be discoverable.
 */
enum class CapabilitySupport(
    val displayText: String,
) {
    SUPPORTED("Supported"),
    UNSUPPORTED("Unsupported"),
    UNKNOWN("Unknown"),
}
