/**
 * Automatically draws expired campsite lotteries.
 * Updates lottery results and entrant notifications
 * in a single Firestore transaction.
 */

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { randomInt } = require("node:crypto");
const logger = require("firebase-functions/logger");

initializeApp();

const db = getFirestore();

/**
 * Randomizes the order of eligible entrants.
 */
function shuffle(entries) {
    for (let i = entries.length - 1; i > 0; i--) {
        const j = randomInt(i + 1);
        [entries[i], entries[j]] = [entries[j], entries[i]];
    }

    return entries;
}

/**
 * Draws a single lottery using a Firestore transaction.
 */
async function drawLottery(lotteryRef) {
    return db.runTransaction(async (transaction) => {
        const lotteryDoc = await transaction.get(lotteryRef);

        if (!lotteryDoc.exists) {
            return false;
        }

        const lottery = lotteryDoc.data();
        const now = Timestamp.now();

        if (
            lottery.status !== "open" ||
            !lottery.registrationClosesAt ||
            lottery.registrationClosesAt.toMillis() > now.toMillis()
        ) {
            return false;
        }

        const entriesSnapshot = await transaction.get(
            lotteryRef.collection("entries")
        );

        const entries = entriesSnapshot.docs;

        if (entries.length !== (lottery.entryCount ?? 0)) {
            throw new Error("Lottery entry count mismatch.");
        }

        const users = new Map();

        // Read all entrant profiles before performing writes.
        for (const entry of entries) {
            const entrantId = entry.data().entrantId;

            const userDoc = await transaction.get(
                db.collection("users").doc(entrantId)
            );

            users.set(
                entrantId,
                userDoc.exists ? userDoc.data() : null
            );
        }

        const eligibleEntries = entries.filter((entry) => {
            const user = users.get(entry.data().entrantId);
            return user && user.status === "active";
        });

        shuffle(eligibleEntries);

        const winnerCount = Math.min(
            lottery.winnerCount ?? 1,
            eligibleEntries.length
        );

        const winners = new Set(
            eligibleEntries
                .slice(0, winnerCount)
                .map((entry) => entry.data().entrantId)
        );

        for (const entry of entries) {
            const entrantId = entry.data().entrantId;
            const user = users.get(entrantId);
            const won = winners.has(entrantId);

            transaction.update(entry.ref, {
                status: won ? "selected" : "not_selected",
                selectedAt: won ? now : null,
                updatedAt: now
            });

            if (
                user &&
                user.status === "active" &&
                (user.notificationsEnabled ?? true)
            ) {
                const notificationRef = db
                    .collection("notifications")
                    .doc();

                transaction.set(notificationRef, {
                    senderId: lottery.ownerId,
                    recipientId: entrantId,
                    type: won ? "lottery_won" : "lottery_lost",
                    title: won
                        ? "You won the lottery!"
                        : "Lottery result",
                    body: won
                        ? `You can reserve a stay from ${lottery.title}.`
                        : `You were not selected for ${lottery.title}.`,
                    lotteryId: lotteryRef.id,
                    reservationId: null,
                    readAt: null,
                    deliveryStatus: "in_app",
                    createdAt: now,
                    updatedAt: now
                });
            }
        }

        transaction.update(lotteryRef, {
            status: "drawn",
            drawnAt: now,
            updatedAt: now
        });

        return true;
    });
}

/**
 * Checks for expired lotteries every five minutes.
 * Runs independently of the Android application.
 */
exports.automaticLotteryDraw = onSchedule(
    {
        schedule: "every 5 minutes",
        maxInstances: 1,
        timeoutSeconds: 120
    },
    async () => {
        const now = Timestamp.now();

        const snapshot = await db
            .collection("lotteries")
            .where("status", "==", "open")
            .get();

        for (const lotteryDoc of snapshot.docs) {
            const closesAt = lotteryDoc.data().registrationClosesAt;

            if (!closesAt || closesAt.toMillis() > now.toMillis()) {
                continue;
            }

            try {
                const drawn = await drawLottery(lotteryDoc.ref);

                if (drawn) {
                    logger.info(`Lottery ${lotteryDoc.id} drawn.`);
                }
            } catch (error) {
                logger.error(
                    `Failed to draw lottery ${lotteryDoc.id}`,
                    error
                );
            }
        }
    }
);