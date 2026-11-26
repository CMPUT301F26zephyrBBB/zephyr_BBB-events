package com.example.zephyr_bbb.database.repository

import android.net.Uri
import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.ImageRecord
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
/**
 * Uploads images to Firebase Storage and saves image metadata in Firestore.
 * Profile images use targetType "profile".
 * Campsite images use targetType "campsite".
 * Call after sign-in.
 */
class ImageRepository {

    private val db = FirebaseProvider.db
    private val storage = FirebaseStorage.getInstance()

    private val imagesRef = db.collection(Collections.IMAGES)

    /**
     * Uploads an image and links it to a profile or campsite.
     * For profiles, targetId is the user's ID.
     * For campsites, targetId is the campsite's ID.
     * Returns the image metadata document ID.
     */
    fun uploadImage(
        uri: Uri,
        targetType: String,
        targetId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseProvider.requireUid()
        val imageRef = imagesRef.document()

        val fileRef = storage.reference
            .child("images/$uid/${imageRef.id}")

        val targetRef = targetReference(targetType, targetId)
        val field = imageField(targetType)

        fileRef.putFile(uri)
            .addOnSuccessListener { upload ->
                val now = Timestamp.now()
                val image = ImageRecord(
                    uploaderId = uid,
                    targetType = targetType,
                    targetId = targetId,
                    storagePath = fileRef.path,
                    contentType = upload.metadata?.contentType
                        ?: "image/jpeg",
                    status = "active",
                    createdAt = now,
                    updatedAt = now
                )
                db.runTransaction { transaction ->
                    val target = transaction.get(targetRef)
                    check(target.exists()) {
                        "Image target does not exist."
                    }
                    val oldImageId = target.getString(field)
                    transaction.set(imageRef, image)
                    transaction.update(
                        targetRef,
                        mapOf(
                            field to imageRef.id,
                            "updatedAt" to now
                        )
                    )
                    oldImageId
                }
                    .addOnSuccessListener { oldImageId ->
                        if (!oldImageId.isNullOrBlank() &&
                            oldImageId != imageRef.id
                        ) {
                            deleteImage(
                                imageId = oldImageId,
                                onSuccess = {},
                                onFailure = { error ->
                                    android.util.Log.w(
                                        "ImageRepository",
                                        "Previous image cleanup failed.",
                                        error
                                    )
                                }
                            )
                        }

                        onSuccess(imageRef.id)
                    }
                    .addOnFailureListener { error ->
                        fileRef.delete()
                        onFailure(error)
                    }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Reads an image's metadata.
     * Returns null if the image record does not exist.
     */
    fun getImage(
        imageId: String,
        onSuccess: (ImageRecord?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        imagesRef.document(imageId)
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObject(ImageRecord::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Returns a download URL that the screen can use to display
     * the image. Returns null if the image record does not exist.
     */
    fun getImageUrl(
        imageId: String,
        onSuccess: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getImage(
            imageId = imageId,
            onSuccess = { image ->
                if (image == null) {
                    onSuccess(null)
                } else {
                    storage.reference
                        .child(image.storagePath)
                        .downloadUrl
                        .addOnSuccessListener {
                            onSuccess(it.toString())
                        }
                        .addOnFailureListener { onFailure(it) }
                }
            },
            onFailure = onFailure
        )
    }

    /**
     * Deletes the stored image and its metadata.
     * Clears the profile/campsite reference if it still points
     * to this image.
     * Handles files that have already been deleted from Storage.
     */
    fun deleteImage(
        imageId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getImage(
            imageId = imageId,
            onSuccess = { image ->
                if (image == null) {
                    onSuccess()
                } else {
                    val targetRef = targetReference(
                        image.targetType,
                        image.targetId
                    )
                    val field = imageField(image.targetType)
                    val imageRef = imagesRef.document(imageId)
                    fun removeMetadata() {
                        db.runTransaction { transaction ->
                            val target = transaction.get(targetRef)
                            transaction.delete(imageRef)
                            if (target.exists() &&
                                target.getString(field) == imageId
                            ) {
                                transaction.update(
                                    targetRef,
                                    mapOf(
                                        field to null,
                                        "updatedAt" to Timestamp.now()
                                    )
                                )
                            }

                            true
                        }
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    }

                    storage.reference
                        .child(image.storagePath)
                        .delete()
                        .addOnSuccessListener {
                            removeMetadata()
                        }
                        .addOnFailureListener { error ->
                            if (error is StorageException &&
                                error.errorCode ==
                                StorageException.ERROR_OBJECT_NOT_FOUND
                            ) {
                                removeMetadata()
                            } else {
                                onFailure(error)
                            }
                        }
                }
            },
            onFailure = onFailure
        )
    }

    private fun targetReference(
        targetType: String,
        targetId: String
    ): DocumentReference {
        val collection = if (targetType == "profile") {
            Collections.USERS
        } else {
            Collections.CAMPSITES
        }

        return db.collection(collection).document(targetId)
    }

    private fun imageField(targetType: String): String {
        return if (targetType == "profile") {
            "profileImageId"
        } else {
            "promoImageId"
        }
    }
}