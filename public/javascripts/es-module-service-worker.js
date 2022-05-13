// In service worker script.
// https://firebase.google.com/docs/auth/web/service-worker-sessions?hl=ja

// Import the functions you need from the SDKs you need
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.8.1/firebase-app.js";
import { getAuth, onAuthStateChanged, getIdToken } from 'https://www.gstatic.com/firebasejs/9.8.1/firebase-auth.js';

// Your web app's Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyAFESs9ah7X6Q-cSSifw1QsmWJLUTJeRcI",
    authDomain: "fir-pac4j-test.firebaseapp.com",
    projectId: "fir-pac4j-test",
    storageBucket: "fir-pac4j-test.appspot.com",
    messagingSenderId: "952736982128",
    appId: "1:952736982128:web:f030d13304010ccc7dc0b5"
};

// Initialize the Firebase app in the service worker script.
initializeApp(firebaseConfig);

// [START auth_svc_subscribe_modular]
/**
 * Returns a promise that resolves with an ID token if available.
 * @return {!Promise<?string>} The promise that resolves with an ID token if
 *     available. Otherwise, the promise resolves with null.
 */
const auth = getAuth();
const getIdTokenPromise = () => {
    return new Promise((resolve, reject) => {
        const unsubscribe = onAuthStateChanged(auth, (user) => {
            unsubscribe();
            if (user) {
                getIdToken(user).then((idToken) => {
                    resolve(idToken);
                }, (error) => {
                    resolve(null);
                });
            } else {
                resolve(null);
            }
        });
    });
};
// [END auth_svc_subscribe_modular]


// [START auth_svc_intercept_modular]
const getOriginFromUrl = (url) => {
    // https://stackoverflow.com/questions/1420881/how-to-extract-base-url-from-a-string-in-javascript
    const pathArray = url.split('/');
    const protocol = pathArray[0];
    const host = pathArray[2];
    return protocol + '//' + host;
};

// Get underlying body if available. Works for text and json bodies.
const getBodyContent = (req) => {
    return Promise.resolve().then(() => {
        if (req.method !== 'GET') {
            if (req.headers.get('Content-Type').indexOf('json') !== -1) {
                return req.json()
                    .then((json) => {
                        return JSON.stringify(json);
                    });
            } else {
                return req.text();
            }
        }
    }).catch((error) => {
        // Ignore error.
    });
};

self.addEventListener('fetch', (event) => {
    /** @type {FetchEvent} */
    const evt = event;

    const requestProcessor = (idToken) => {
        let req = evt.request;
        let processRequestPromise = Promise.resolve();
        // For same origin https requests, append idToken to header.
        if (self.location.origin == getOriginFromUrl(evt.request.url) &&
            (self.location.protocol == 'https:' ||
                self.location.hostname == 'localhost') &&
            idToken) {
            // Clone headers as request headers are immutable.
            const headers = new Headers();
            req.headers.forEach((val, key) => {
                headers.append(key, val);
            });
            // Add ID token to header.
            headers.append('Authorization', 'Bearer ' + idToken);
            processRequestPromise = getBodyContent(req).then((body) => {
                try {
                    req = new Request(req.url, {
                        method: req.method,
                        headers: headers,
                        mode: 'same-origin',
                        credentials: req.credentials,
                        cache: req.cache,
                        redirect: req.redirect,
                        referrer: req.referrer,
                        body,
                        // bodyUsed: req.bodyUsed,
                        // context: req.context
                    });
                } catch (e) {
                    // This will fail for CORS requests. We just continue with the
                    // fetch caching logic below and do not pass the ID token.
                }
            });
        }
        return processRequestPromise.then(() => {
            return fetch(req);
        });
    };
    // Fetch the resource after checking for the ID token.
    // This can also be integrated with existing logic to serve cached files
    // in offline mode.
    evt.respondWith(getIdTokenPromise().then(requestProcessor, requestProcessor));
});
// [END auth_svc_intercept_modular]
