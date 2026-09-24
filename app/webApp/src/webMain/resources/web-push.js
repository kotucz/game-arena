window.GameArenaPush = (() => {
  const firebaseSdk = 'https://www.gstatic.com/firebasejs/10.14.0';
  let firebaseMessaging = null;
  let firebaseMessagingModule = null;

  return {
    async fetchWebPushToken(onResult) {
      try {
        const configResponse = await fetch('/api/firebase-config');
        if (!configResponse.ok) {
          console.warn('Failed to load Firebase config:', configResponse.status);
          onResult(null);
          return null;
        }

        const config = await configResponse.json();
        if (!config.vapidKey) {
          console.warn('Firebase web VAPID key is missing.');
          onResult(null);
          return null;
        }

        if (firebaseMessaging === null) {
          const [appModule, messagingModule] = await Promise.all([
            import(firebaseSdk + '/firebase-app.js'),
            import(firebaseSdk + '/firebase-messaging.js'),
          ]);
          const app = appModule.getApps().length > 0
            ? appModule.getApp()
            : appModule.initializeApp(config);
          firebaseMessaging = messagingModule.getMessaging(app);
          firebaseMessagingModule = messagingModule;
        }

        const registration = await navigator.serviceWorker.ready;
        const token = await firebaseMessagingModule.getToken(firebaseMessaging, {
          vapidKey: config.vapidKey,
          serviceWorkerRegistration: registration,
        });
        onResult(token);
        return token;
      } catch (error) {
        console.warn('Failed to obtain web push token', error);
        onResult(null);
        return null;
      }
    },
  };
})();
