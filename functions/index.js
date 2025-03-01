const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.notifyGradeChange = functions.database
    .ref('/grades/{semesterId}/data/{gradeId}')
    .onUpdate((change, context) => {
        const before = change.before.val();
        const after = change.after.val();

        if (before.finalgrade !== after.finalgrade || before.remarks !== after.remarks) {
            const studentNumber = after.studnum;

            // Fetch the student's FCM token
            return admin.database().ref(`/normalLogin/students/${studentNumber}/fcmtoken`).once('value')
                .then(snapshot => {
                    const token = snapshot.val();
                    if (token) {
                        const message = {
                            notification: {
                                title: 'Grade Update',
                                body: `Your grade for subject ${after.id} has been updated. Final grade: ${after.finalgrade}, Remarks: ${after.remarks}`
                            },
                            token: token
                        };
                        return admin.messaging().send(message);
                    }
                }).catch(error => {
                    console.error('Error fetching FCM token:', error);
                });
        }

        return null;
    });
