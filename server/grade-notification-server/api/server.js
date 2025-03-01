const express = require('express');
const admin = require('firebase-admin');
const app = express();
const PORT = process.env.PORT || 3000;

// Replace with the path to your Firebase service account key JSON file
const serviceAccount = require('../serviceAccountKey.json'); // Relative to api/ folder

// Initialize Firebase Admin SDK
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://icstmgsdb-default-rtdb.firebaseio.com"  // Replace with your database URL
});

const db = admin.database();

// Listen to changes in grades
db.ref('/grades/grade2425_1stsem/data').on('child_changed', (snapshot) => {
    const updatedGrade = snapshot.val();
    const studentId = updatedGrade.studnum;  // Access the student number field inside the data

    if (!studentId) {
        console.log('Student ID not found in the snapshot');
        return;
    }

    // Check for specific fields and conditions
    const finalGrade = updatedGrade.finalgrade;
    const remarks = updatedGrade.remarks;

    // Retrieve the student's FCM token
    db.ref(`/normalLogin/students/${studentId}/fcmtoken`).once('value')
        .then((tokenSnapshot) => {
            const fcmToken = tokenSnapshot.val();
            console.log(`FCM token for student ${studentId}: ${fcmToken}`); // Log retrieved token

            if (!fcmToken) {
                console.log(`No FCM token found for student ${studentId}`);
                return;
            }

            // Create a message payload using the updated v1 format
            const message = {
                token: fcmToken,
                notification: {
                    title: 'Grade Update',
                    body: `Your grade has been updated. Final Grade: ${finalGrade}. Remarks: ${remarks}`,
                },
                android: {
                    priority: "high"
                },
                apns: {
                    headers: {
                        "apns-priority": "10"
                    }
                },
                data: {
                    title: 'Grade Update',
                    body: `Your grade has been updated. Final Grade: ${finalGrade}. Remarks: ${remarks}`,
                },
            };                   

            // Send the notification using Firebase Admin SDK (v1 API)
            admin.messaging().send(message)
                .then((response) => {
                    console.log(`Notification sent to ${studentId} successfully. Response:`, response);
                })
                .catch((error) => {
                    console.error('Error sending notification:', error);
                });
        });

});

// Set up a basic route to check if the server is running
app.get('/', (req, res) => {
    res.send('Firebase Grade Notification Server is running!');
});

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
