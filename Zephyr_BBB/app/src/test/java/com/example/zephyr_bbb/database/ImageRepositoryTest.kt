package com.example.zephyr_bbb.database

import android.net.Uri
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.ImageRecord
import com.example.zephyr_bbb.database.repository.ImageRepository
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.storage.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ImageRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var repository: ImageRepository
    private lateinit var transaction: Transaction
    private lateinit var imagesRef: CollectionReference
    private lateinit var imageRef: DocumentReference
    private lateinit var targetRef: DocumentReference
    private lateinit var rootRef: StorageReference
    private lateinit var fileRef: StorageReference
    private lateinit var uri: Uri

    private val path = "images/test_user/test_image"

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)
        mockkStatic(FirebaseStorage::class)

        db = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        imagesRef = mockk(relaxed = true)
        imageRef = mockk(relaxed = true)
        targetRef = mockk(relaxed = true)
        rootRef = mockk(relaxed = true)
        fileRef = mockk(relaxed = true)
        uri = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_user"
        every { FirebaseStorage.getInstance() } returns storage

        every { db.collection("images") } returns imagesRef
        every { imagesRef.document() } returns imageRef
        every { imagesRef.document("test_image") } returns imageRef
        every { imageRef.id } returns "test_image"

        every { storage.reference } returns rootRef
        every { rootRef.child(path) } returns fileRef
        every { fileRef.path } returns path

        every {
            db.collection("campsites").document("test_campsite")
        } returns targetRef

        every {
            db.collection("users").document("test_user")
        } returns targetRef

        repository = ImageRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
        unmockkStatic(FirebaseStorage::class)
    }

    private fun <T> successfulTask(result: T): Task<T> {
        val task = mockk<Task<T>>(relaxed = true)

        every { task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<T>>().onSuccess(result)
            task
        }

        return task
    }

    private fun mockImageRecord(image: ImageRecord) {
        val snapshot = mockk<DocumentSnapshot>()

        every {
            snapshot.toObject(ImageRecord::class.java)
        } returns image

        every { imageRef.get() } returns successfulTask(snapshot)
    }

    private fun mockUpload(field: String) {
        val uploadTask = mockk<UploadTask>(relaxed = true)
        val uploadSnapshot = mockk<UploadTask.TaskSnapshot>()
        val metadata = mockk<StorageMetadata>()

        every { metadata.contentType } returns "image/jpeg"
        every { uploadSnapshot.metadata } returns metadata
        every { fileRef.putFile(uri) } returns uploadTask

        every {
            uploadTask.addOnSuccessListener(any())
        } answers {
            firstArg<OnSuccessListener<UploadTask.TaskSnapshot>>()
                .onSuccess(uploadSnapshot)
            uploadTask
        }

        every { transaction.get(targetRef).exists() } returns true
        every { transaction.get(targetRef).getString(field) } returns null

        every { db.runTransaction<String?>(any()) } answers {
            val result = firstArg<Transaction.Function<String?>>()
                .apply(transaction)

            successfulTask(result)
        }
    }

    // Uploading a campsite image saves and links its metadata
    @Test
    fun testUploadCampsiteImage() {
        mockUpload("promoImageId")

        var result: String? = null

        repository.uploadImage(
            uri,
            "campsite",
            "test_campsite",
            { result = it },
            { throw it }
        )

        assertEquals("test_image", result)

        verify(exactly = 1) { fileRef.putFile(uri) }

        verify(exactly = 1) {
            transaction.set(imageRef, match<ImageRecord> {
                it.uploaderId == "test_user" &&
                        it.targetType == "campsite" &&
                        it.targetId == "test_campsite" &&
                        it.storagePath == path &&
                        it.contentType == "image/jpeg" &&
                        it.status == "active"
            })
        }

        verify(exactly = 1) {
            transaction.update(targetRef, match<Map<String, Any?>> {
                it["promoImageId"] == "test_image"
            })
        }
    }

    // Uploading a profile image updates the user's image reference
    @Test
    fun testUploadProfileImage() {
        mockUpload("profileImageId")

        repository.uploadImage(
            uri,
            "profile",
            "test_user",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(imageRef, match<ImageRecord> {
                it.targetType == "profile" &&
                        it.targetId == "test_user"
            })
        }

        verify(exactly = 1) {
            transaction.update(targetRef, match<Map<String, Any?>> {
                it["profileImageId"] == "test_image"
            })
        }
    }

    // Image metadata can be retrieved from Firestore
    @Test
    fun testGetImage() {
        val image = ImageRecord(
            id = "test_image",
            storagePath = path,
            targetType = "campsite",
            targetId = "test_campsite"
        )

        mockImageRecord(image)

        var result: ImageRecord? = null

        repository.getImage(
            "test_image",
            { result = it },
            { throw it }
        )

        assertEquals(image, result)
        verify(exactly = 1) { imageRef.get() }
    }

    // Image metadata is used to retrieve the Storage download URL
    @Test
    fun testGetImageUrl() {
        mockImageRecord(
            ImageRecord(id = "test_image", storagePath = path)
        )

        val downloadUri = mockk<Uri>()
        val expectedUrl = "https://example.com/test_image.jpg"

        every { downloadUri.toString() } returns expectedUrl
        every { fileRef.downloadUrl } returns successfulTask(downloadUri)

        var result: String? = null

        repository.getImageUrl(
            "test_image",
            { result = it },
            { throw it }
        )

        assertEquals(expectedUrl, result)
        verify(exactly = 1) { fileRef.downloadUrl }
    }

    // Deleting an image removes its file, metadata and linked reference
    @Test
    fun testDeleteImage() {
        mockImageRecord(
            ImageRecord(
                id = "test_image",
                targetType = "campsite",
                targetId = "test_campsite",
                storagePath = path
            )
        )

        every { fileRef.delete() } returns successfulTask<Void>(nullResult())

        every { transaction.get(targetRef).exists() } returns true

        every {
            transaction.get(targetRef).getString("promoImageId")
        } returns "test_image"

        every { db.runTransaction<Boolean>(any()) } answers {
            val result = firstArg<Transaction.Function<Boolean>>()
                .apply(transaction)

            successfulTask(result)
        }

        var success = false

        repository.deleteImage(
            "test_image",
            { success = true },
            { throw it }
        )

        assertTrue(success)

        verify(exactly = 1) { fileRef.delete() }
        verify(exactly = 1) { transaction.delete(imageRef) }

        verify(exactly = 1) {
            transaction.update(targetRef, match<Map<String, Any?>> {
                it.containsKey("promoImageId") &&
                        it["promoImageId"] == null
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> nullResult(): T = null as T
}
