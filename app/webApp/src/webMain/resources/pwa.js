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

      // Attempt to subscribe for web push if permission available/granted
      try {
        if (Notification && Notification.permission !== 'denied') {
          if (Notification.permission !== 'granted') {
            await Notification.requestPermission();
          }

          if (Notification.permission === 'granted') {
            // Fetch VAPID public key from server (may be empty if not configured)
            const resp = await fetch('/api/notifications/vapidPublicKey');
            const vapidKey = (await resp.text()).trim();
            let applicationServerKey = null;
            if (vapidKey) {
              // Convert base64 URL-safe to Uint8Array
              const padding = '='.repeat((4 - (vapidKey.length % 4)) % 4);
              const base64 = (vapidKey + padding).replace(/-/g, '+').replace(/_/g, '/');
              const rawData = window.atob(base64);
              const outputArray = new Uint8Array(rawData.length);
              for (let i = 0; i < rawData.length; ++i) {
                outputArray[i] = rawData.charCodeAt(i);
              }
              applicationServerKey = outputArray;
            }

            // Subscribe (applicationServerKey may be null for some setups)
            const subscription = await registration.pushManager.subscribe({
              userVisibleOnly: true,
              applicationServerKey,
            }).catch((e) => {
              console.warn('PushManager.subscribe failed', e);
              return null;
            });

            if (subscription) {
              const subscriptionJson = JSON.stringify(subscription.toJSON());

              // Use a client-generated tokenId stored in localStorage
              let tokenId = localStorage.getItem('pushTokenId');
              if (!tokenId) {
                tokenId = crypto.randomUUID();
                localStorage.setItem('pushTokenId', tokenId);
              }

              // Register subscription with server
              try {
                await fetch(`/api/notifications/tokens/${encodeURIComponent(tokenId)}`, {
                  method: 'PUT',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ service: 'webpush', token: subscriptionJson }),
                });
                console.log('Web push subscription sent to server');
              } catch (e) {
                console.warn('Failed to register web push subscription with server', e);
              }
            }
          }
        }
      } catch (e) {
        console.warn('Web push subscription flow failed', e);
      }
    } catch (error) {
      console.error('Service worker registration failed:', error);
    }
  });
}
