# Database Functions

All functions also take `onSuccess` and `onFailure` callbacks. IDs and data are passed to `onSuccess`, rather than returned directly. The two `observe` functions directly return `ListenerRegistration`; call `.remove()` when finished listening.

Dates use `YYYY-MM-DD`, with checkout excluded. Missing availability dates count as unavailable. Sign in before using functions that operate on the current user.

## UserRepository

| Function                     | Parameters                                 | Use                                                                                                                              |
|------------------------------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| getOrCreateCurrentUser       | None                                       | Sign in anonymously or reuse the current account, then load/create its profile.                                                  |
| getUser                      | userId: String                             | Get a user's profile.                                                                                                            |
| updateProfile                | name: String, email: String, phone: String | Update the current user's personal information.                                                                                  |
| updateNotificationPreference | enabled: Boolean                           | Enable or disable newly created in-app notifications.                                                                            |
| updateProfileImage           | imageId: String?                           | Set the profile image reference; null clears it. Does not upload or delete image files.                                          |
| deleteCurrentProfile         | None                                       | Delete the Firestore profile and sign out. Does not delete the Authentication account, reservations, listings, or image records. |

## CampsiteRepository

| Function        | Parameters                                                                                                                  | Use                                                                                                                  |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| createCampsite  | campsite: Campsite                                                                                                          | Create a campsite; passes its ID to onSuccess.                                                                       |
| getCampsite     | campsiteId: String                                                                                                          | Get campsite details.                                                                                                |
| getMyCampsites  | None                                                                                                                        | Get the current owner's campsites.                                                                                   |
| searchCampsites | searchText: String = "", location: String = "", guests: Long = 1, checkInDate: String? = null, checkOutDate: String? = null | Browse or filter campsites. Supply both dates or neither. With dates, every requested night must be explicitly open. |
| updateCampsite  | campsite: Campsite                                                                                                          | Update the current owner's campsite. Include its existing ID in the model.                                           |

## AvailabilityRepository

| Function                | Parameters                                              | Use                                                                                  |
|-------------------------|---------------------------------------------------------|--------------------------------------------------------------------------------------|
| getAvailability         | campsiteId: String                                      | Get the campsite's configured nights.                                                |
| setAvailability         | campsiteId: String, date: String, status: String        | Owner sets one night to "open" or "blocked". Cannot overwrite reserved nights.       |
| setAvailabilityForDates | campsiteId: String, dates: List<String>, status: String | Owner sets multiple nights to "open" or "blocked". Cannot overwrite reserved nights. |

## LotteryRepository

Automatic lottery scheduling is not implemented in this repository. drawWinners must be called after registration closes. It doesn't automatically run when the deadline passes.

| Function                      | Parameters         | Use                                                                     |
|-------------------------------|--------------------|-------------------------------------------------------------------------|
| createLottery                 | lottery: Lottery   | Create a lottery; passes its ID to onSuccess.                           |
| updateLottery                 | lottery: Lottery   | Update the owner's undrawn lottery. Include its existing ID.            |
| getLottery                    | lotteryId: String  | Get lottery details.                                                    |
| getMyLotteries                | None               | Get the current owner's lotteries.                                      |
| getActiveLotteriesForCampsite | campsiteId: String | Get lotteries currently accepting registrations for a campsite.         |
| getWaitlist                   | lotteryId: String  | Get the lottery's entries and their statuses.                           |
| getMyEntry                    | lotteryId: String  | Get the current user's entry/result.                                    |
| joinWaitlist                  | lotteryId: String  | Join the lottery's waiting list during registration if capacity allows. |
| leaveWaitlist                 | lotteryId: String  | Leave an undrawn lottery's waiting list.                                |
| drawWinners                   | lotteryId: String  | Draw after registration closes; passes winner user IDs to onSuccess.    |
| decline                       | lotteryId: String  | Decline an unused winning opportunity.                                  |

## ReservationRepository

| Function             | Parameters                                                                                                                 | Use                                                                                                                                                                                                                                                              |
|----------------------|----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| createReservation    | lotteryId: String, campsiteId: String, checkInDate: String, checkOutDate: String, guestCount: Long, rulesAccepted: Boolean | Book using an unused winning entry; passes reservation ID to onSuccess. Requires rulesAccepted = true, an open booking period, a valid guest count, and open nights within the lottery's stay period at an included campsite. Automatically reserves the nights. |
| getReservation       | reservationId: String                                                                                                      | Get reservation details.                                                                                                                                                                                                                                         |
| getMyReservations    | None                                                                                                                       | Get the current entrant's reservations.                                                                                                                                                                                                                          |
| getOwnerReservations | None                                                                                                                       | Get reservations for the current owner's campsites.                                                                                                                                                                                                              |
| cancelReservation    | reservationId: String                                                                                                      | Entrant cancels their confirmed reservation and releases its nights. Does not restore the used lottery opportunity.                                                                                                                                              |
| rejectReservation    | reservationId: String, reason: String                                                                                      | Owner rejects a confirmed reservation and releases its nights. Does not restore the used lottery opportunity.                                                                                                                                                    |

## ImageRepository
Image upload, download URL retrieval, and file deletion need Firebase Storage. The functions are implemented but the storage is not configured.

| Function    | Parameters                                     | Use                                                                                            |
|-------------|------------------------------------------------|------------------------------------------------------------------------------------------------|
| uploadImage | uri: Uri, targetType: String, targetId: String | Upload and link an image; passes image ID to onSuccess. targetType is "profile" or "campsite". |
| getImage    | imageId: String                                | Get image metadata.                                                                            |
| getImageUrl | imageId: String                                | Get the image's display/download URL.                                                          |
| deleteImage | imageId: String                                | Delete the image file, metadata, and matching profile/campsite reference.                      |

## NotificationRepository

| Function                 | Parameters                                     | Use                                                                                                                                                                 |
|--------------------------|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| getMyNotifications       | None                                           | Get the current user's received in-app notifications.                                                                                                               |
| observeMyNotifications   | None                                           | Listen for in-app notifications in real time; returns ListenerRegistration.                                                                                         |
| getSentNotifications     | None                                           | Get notifications sent under the current user's ID.                                                                                                                 |
| sendWaitlistNotification | lotteryId: String, title: String, body: String | Owner sends an in-app announcement to lottery entrants with active profiles and notifications enabled, regardless of entry status. Does not send phone push alerts. |

## MessageRepository

| Function                      | Parameters                          | Use                                                                         |
|-------------------------------|-------------------------------------|-----------------------------------------------------------------------------|
| sendMessage                   | reservationId: String, body: String | Owner messages the reservation's entrant; passes message ID to onSuccess.   |
| getMyMessages                 | None                                | Get messages received by the current entrant.                               |
| getMessagesForReservation     | reservationId: String               | Get messages about a reservation.                                           |
| observeMessagesForReservation | reservationId: String               | Listen for reservation messages in real time; returns ListenerRegistration. |

## RatingRepository

| Function     | Parameters                      | Use                                                  |
|--------------|---------------------------------|------------------------------------------------------|
| rateCampsite | campsiteId: String, score: Long | Create/update the current user's rating from 1 to 5. |
| getMyRating  | campsiteId: String              | Get the current user's rating for a campsite.        |
| getRatings   | campsiteId: String              | Get all ratings for a campsite.                      |

## AdminRepository

These functions require an active profile with the admin role.

| Function            | Parameters         | Use                                                                                                                                                |
|---------------------|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| getCampsites        | None               | Browse all campsite listings.                                                                                                                      |
| getUsers            | None               | Browse active profiles.                                                                                                                            |
| getImages           | None               | Browse image metadata.                                                                                                                             |
| getNotificationLogs | None               | Get all saved notification records.                                                                                                                |
| getMessageLogs      | None               | Get all saved owner messages.                                                                                                                      |
| removeCampsite      | campsiteId: String | Remove a listing and its lottery references. Cancel lotteries left with no campsites. Existing reservations and historical subcollections remain.  |
| removeProfile       | userId: String     | Mark a profile removed, clear personal details, remove owned listings, and cancel owned lotteries. Reservations, lottery entries, and logs remain. |
| removeOwner         | ownerId: String    | Remove an owner using the same operation as removeProfile.                                                                                         |
| removeImage         | imageId: String    | Remove image metadata and its matching reference; the actual image file remains.                                                                   |