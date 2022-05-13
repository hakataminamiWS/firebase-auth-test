// Import the functions you need from the SDKs you need
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.8.1/firebase-app.js";
import {
    getAuth,
    signInWithCustomToken,
    updateProfile,
    getIdToken,
    reload,
    getIdTokenResult
} from 'https://www.gstatic.com/firebasejs/9.8.1/firebase-auth.js';

// Your web app's Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyAFESs9ah7X6Q-cSSifw1QsmWJLUTJeRcI",
    authDomain: "fir-pac4j-test.firebaseapp.com",
    projectId: "fir-pac4j-test",
    storageBucket: "fir-pac4j-test.appspot.com",
    messagingSenderId: "952736982128",
    appId: "1:952736982128:web:f030d13304010ccc7dc0b5"
};

// Initialize Firebase
initializeApp(firebaseConfig);

window.addEventListener('load', () => {

    // query parameter
    const param = window.location.search;
    const url = `/get-id-token${param}`;

    let redirectUrl = "http://localhost:9000/";
    const auth = getAuth();

    // fetch with the query parameter for getting firebase idToken 
    fetch(url, {
        method: 'GET',
        mode: 'same-origin',
        credentials: "same-origin",
    })
        .then((response) => response.json())
        .then((jsonObj) => {
            redirectUrl = jsonObj.redirectUrl;
            return jsonObj.firebaseIdToken;
        })
        // Signed in
        .then((idToken) => {
            return signInWithCustomToken(auth, idToken);
        })
        // Update a user's profile with LINE name and LINE picture
        .then((userCredential) => {
            const user = userCredential.user;
            return getIdTokenResult(user)
                .then((idTokenResult) => {
                    const displayName = idTokenResult.claims.line_name;
                    const photoURL = idTokenResult.claims.line_picture;
                    // https://firebase.google.com/docs/auth/web/manage-users#update_a_users_profile
                    return updateProfile(user, {
                        displayName: displayName, photoURL: photoURL
                    });
                })
        })
        // update currentUser and idToken
        .then(() => {
            return reload(auth.currentUser).then(() => getIdToken(auth.currentUser, true));
        })
        .then(() => {
            location.href = redirectUrl;
        })
        .catch((error) => {
            const errorCode = error.code;
            const errorMessage = error.message;
            console.log(errorCode);
            console.log(errorMessage);
            location.href = redirectUrl;
        })
});