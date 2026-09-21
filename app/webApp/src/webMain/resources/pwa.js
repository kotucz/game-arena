const installButtonId = 'gamearena-install-button';

function ensureInstallButton() {
  if (document.getElementById(installButtonId)) {
    return document.getElementById(installButtonId);
  }

  const button = document.createElement('button');
  button.id = installButtonId;
  button.type = 'button';
  button.textContent = 'Install app';
  button.setAttribute('aria-label', 'Install Game Arena');
  button.className = 'pwa-install-button';
  button.style.display = 'none';
  document.body.appendChild(button);
  return button;
}

let deferredPrompt = null;

window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault();
  deferredPrompt = event;

  const button = ensureInstallButton();
  button.style.display = 'inline-flex';

  button.addEventListener('click', async () => {
    if (!deferredPrompt) {
      return;
    }

    button.disabled = true;
    deferredPrompt.prompt();
    await deferredPrompt.userChoice;
    button.style.display = 'none';
    deferredPrompt = null;
    button.disabled = false;
  }, { once: true });
});

if ('serviceWorker' in navigator) {
  window.addEventListener('load', async () => {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js', { scope: '/' });
      console.log('Game Arena service worker registered.');

      // ── FCM push notifications ──────────────────────────────────────────────
      // The Firebase config (including FCM VAPID key) is served by the server so
      // it can be updated via application.conf without rebuilding the JS bundle.
      try {
        if (Notification && Notification.permission !== 'denied') {
          if (Notification.permission !== 'granted') {
            await Notification.requestPermission();
          }

          if (Notification.permission === 'granted') {
            // Fetch Firebase web config + FCM VAPID key from the server.
            // To update: change firebase.webConfig / firebase.webVapidKey in application.conf.
            const configResp = await fetch('/api/firebase-config');
            if (!configResp.ok) {
              console.warn('Failed to load Firebase config from server:', configResp.status);
              return;
            }
            const firebaseConfig = await configResp.json();
            const { vapidKey, ...appConfig } = firebaseConfig;

            // Dynamic import of the Firebase modular SDK (no module type needed on the script tag).
            // Check https://firebase.google.com/docs/web/learn-more#available-libraries for the latest version.
            const FIREBASE_SDK = 'https://www.gstatic.com/firebasejs/10.14.0';
            const { initializeApp }   = await import(`${FIREBASE_SDK}/firebase-app.js`);
            const { getMessaging, getToken, onMessage } = await import(`${FIREBASE_SDK}/firebase-messaging.js`);

            const app       = initializeApp(appConfig);
            const messaging = getMessaging(app);

            // Handle foreground messages (app tab is active).
            onMessage(messaging, (payload) => {
              console.log('FCM foreground message:', payload);
              const { title = 'Game Arena', body = '' } = payload.notification ?? {};
              // TODO: show an in-app toast / notification banner instead of a system notification.
            });

            const token = await getToken(messaging, {
              vapidKey,
              serviceWorkerRegistration: registration,
            }).catch((e) => {
              console.warn('FCM getToken failed', e);
              return null;
            });

            if (token) {
              // Use a client-generated tokenId stored in localStorage so the same device
              // always upserts the same DB row when the FCM token refreshes.
              let tokenId = localStorage.getItem('pushTokenId');
              if (!tokenId) {
                tokenId = crypto.randomUUID();
                localStorage.setItem('pushTokenId', tokenId);
              }

              try {
                await fetch(`/api/notifications/tokens/${encodeURIComponent(tokenId)}`, {
                  method: 'PUT',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ service: 'fcm', token }),
                });
                console.log('FCM token registered with server.');
              } catch (e) {
                console.warn('Failed to register FCM token with server', e);
              }
            }
          }
        }
      } catch (e) {
        console.warn('FCM push setup failed', e);
      }
      // ── end FCM ─────────────────────────────────────────────────────────────

    } catch (error) {
      console.error('Service worker registration failed:', error);
    }
  });
}
