import { initializeApp } from "https://www.gstatic.com/firebasejs/9.8.1/firebase-app.js";
import { getAuth, signOut } from 'https://www.gstatic.com/firebasejs/9.8.1/firebase-auth.js';

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

const auth = getAuth();
signOut(auth).then(() => {
    // Sign-out successful.
}).catch((error) => {
    // An error happened.
});