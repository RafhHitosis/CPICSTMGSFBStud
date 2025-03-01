const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// Listen to changes in grades and send an FCM notification
exports.sendGradeUpdateNotification = functions.database
    .ref('/grades/{semester}/data/{studentId}')
    .onWrite((change, context) => {
        const studentId = context.params.studentId;
        const newValue = change.after.val();

        if (!newValue) {
            return null; // No value change detected
        }

        // Fetch the final grade and remarks
        const finalGrade = newValue.finalgrade;
        const remarks = newValue.remarks;

        // Get the student's FCM token
        return admin.database().ref(`/normalLogin/students/${studentId}/fcmtoken`).once('value')
            .then((snapshot) => {
                const fcmToken = snapshot.val();
                if (!fcmToken) {
                    console.log(`No FCM token found for student ${studentId}`);
                    return null;
                }

                // Create a message payload
                const payload = {
                    notification: {
                        title: 'Grade Update',
                        body: `Your grade has been updated. Final Grade: ${finalGrade}. Remarks: ${remarks}`,
                    },
                };

                // Send the message
                return admin.messaging().sendToDevice(fcmToken, payload)
                    .then(() => {
                        console.log(`Notification sent to ${studentId} successfully.`);
                        return null;
                    })
                    .catch((error) => {
                        console.error('Error sending notification:', error);
                    });
            });
    });
