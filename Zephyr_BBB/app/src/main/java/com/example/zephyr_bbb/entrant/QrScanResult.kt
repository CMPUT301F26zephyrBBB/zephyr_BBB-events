package com.example.zephyr_bbb.entrant

// Checks the text read from a scanned QR code before it is used as a campsite ID. The owner's
// QR code holds the campsite ID (OwnerMyCampsitesScreen) so anything else, like a
// web address from some other QR code is rejected
// Outstanding issues: only the shape of the text is checked, so an ID that matches no
// campsite is caught later by the details screen ("This campsite could not be found.").

/** Message shown when a scanned QR code is not a campsite code. */
const val NOT_A_CAMPSITE_CODE = "This QR code is not a Zephyr campsite code."

/**
 * Turns the text read from a QR code into a campsite ID.
 * Surrounding whitespace is ignored. The text is rejected if it is empty, contains whitespace,
 * or contains a "/" (campsite IDs never do, and it would break the details route).
 *
 * @param rawValue the text decoded from the QR code, or null if the code had no text
 * @return the campsite ID, or null if the text cannot be a campsite ID
 */
fun campsiteIdFromScan(rawValue: String?): String? {
    val id = rawValue?.trim() ?: return null
    if (id.isEmpty() || id.length > MAX_ID_LENGTH) return null
    if (id.any { it.isWhitespace() || it == '/' }) return null
    return id
}

private const val MAX_ID_LENGTH = 128
