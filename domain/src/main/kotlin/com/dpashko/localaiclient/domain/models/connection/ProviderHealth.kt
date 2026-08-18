package com.dpashko.localaiclient.domain.models.connection

/**
 * Reachability state for the currently edited local provider connection.
 */
enum class ProviderHealth(
    /** Short label rendered on the connection screen. */
    val displayText: String,
) {
    /** No health check has been run for the current host and port. */
    NOT_CHECKED("Not checked"),
    /** A health check or model refresh is currently running. */
    CHECKING("Checking"),
    /** Provider responded successfully. */
    REACHABLE("Reachable"),
    /** Provider could not be reached or returned an unusable response. */
    OFFLINE("Offline"),
    /** Provider request timed out. */
    TIMEOUT("Timeout"),
}
