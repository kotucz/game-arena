window.GameArenaPush = (() => {
  const firebaseSdk = 'https://www.gstatic.com/firebasejs/10.14.0';
  let firebaseMessaging = null;
  let firebaseMessagingModule = null;

  return {
    async fetchWebPushToken(onResult) {
      const tokenId = this.persistentWebPushTokenId();
      try {
        const configResponse = await fetch('/api/firebase-config');
        if (!configResponse.ok) {
          console.warn('Failed to load Firebase config:', configResponse.status);
          onResult(null, tokenId);
          return null;
        }

        const config = await configResponse.json();
        if (!config.vapidKey) {
          console.warn('Firebase web VAPID key is missing.');
          onResult(null, tokenId);
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
        onResult(token, tokenId);
        return token;
      } catch (error) {
        console.warn('Failed to obtain web push token', error);
        onResult(null, tokenId);
        return null;
      }
    },

    persistentWebPushTokenId() {
      let tokenId = window.localStorage.getItem('pushTokenId');
      if (!tokenId) {
        tokenId = crypto.randomUUID();
        window.localStorage.setItem('pushTokenId', tokenId);
      }
      return tokenId;
    },
  };
})();
