// Install servicerWorker if supported on sign-in/sign-up page.
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/assets/javascripts/es-module-service-worker.js', { type: 'module', scope: '/' })
    .catch(() => {
      // work around for service worker type='module' not support
      navigator.serviceWorker.register('/assets/javascripts/import-service-worker.js', { scope: '/' });
    });
}