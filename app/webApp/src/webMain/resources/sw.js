// ── Firebase Messaging (background push) ────────────────────────────────────
// Firebase compat scripts are required in service workers because SW module
// imports have limited browser support. Keep the version in sync with web-push.js.
// Check https://firebase.google.com/docs/web/learn-more#available-libraries for latest.
const FIREBASE_SDK = 'https://www.gstatic.com/firebasejs/10.14.0';
importScripts(`${FIREBASE_SDK}/firebase-app-compat.js`);
importScripts(`${FIREBASE_SDK}/firebase-messaging-compat.js`);

// Firebase web SDK config — mirrors application.conf firebase.webConfig.
// Update both here AND in application.conf when the Firebase project settings change.
const FIREBASE_CONFIG = {
  apiKey:            'AIzaSyCqdx5USEs15DlBFlfyxkku2O9ly62b46g',
  authDomain:        'game-arena-c1035.firebaseapp.com',
  projectId:         'game-arena-c1035',
  storageBucket:     'game-arena-c1035.firebasestorage.app',
  messagingSenderId: '541489886592',
  appId:             '1:541489886592:web:2920f01ffd5a537838bd65',
};

firebase.initializeApp(FIREBASE_CONFIG);
const messaging = firebase.messaging();

// Background messages — app is not in the foreground tab.
messaging.onBackgroundMessage((payload) => {
  const { title = 'Game Arena', body = '' } = payload.notification ?? {};
  self.registration.showNotification(title, {
    body,
    // TODO: set icon/badge once assets are finalised (e.g. icon: '/icon-192.png')
    data: { url: '/' },
  });
});
// ── end Firebase Messaging ───────────────────────────────────────────────────

const CACHE_NAME = 'gamearena-shell-v2';
const APP_SHELL = [
  '/',
  '/index.html',
  '/styles.css',
  '/manifest.webmanifest',
  '/icon.svg',
  '/web-push.js',
  '/webApp.js',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(
      keys
        .filter((key) => key !== CACHE_NAME)
        .map((key) => caches.delete(key))
    )).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;

  if (request.method !== 'GET') {
    return;
  }

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) {
    return;
  }

  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/auth/')) {
    event.respondWith(
      fetch(request).catch(() => caches.match('/index.html'))
    );
    return;
  }

  const isAppAsset =
    url.pathname === '/' ||
    url.pathname === '/index.html' ||
    url.pathname === '/styles.css' ||
    url.pathname === '/manifest.webmanifest' ||
    url.pathname === '/icon.svg' ||
    url.pathname === '/webApp.js' ||
    url.pathname.endsWith('.js') ||
    url.pathname.endsWith('.wasm') ||
    url.pathname.endsWith('.css') ||
    url.pathname.endsWith('.svg') ||
    url.pathname.endsWith('.png') ||
    url.pathname.endsWith('.ico') ||
    url.pathname.endsWith('.woff') ||
    url.pathname.endsWith('.woff2') ||
    url.pathname.endsWith('.ttf') ||
    url.pathname.endsWith('.otf');

  if (isAppAsset) {
    event.respondWith(
      caches.match(request).then((cached) => {
        if (cached) {
          return cached;
        }

        return fetch(request)
          .then((response) => {
            const responseClone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, responseClone));
            return response;
          })
          .catch(() => caches.match('/index.html'));
      })
    );
    return;
  }

  event.respondWith(
    fetch(request).catch(() => caches.match('/index.html'))
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  // TODO: deep-link navigation — parse event.notification.data (e.g. gameId, gameType)
  //       and navigate to the specific game screen once routing supports deep links.
  const url = event.notification.data?.url ?? '/';
  event.waitUntil(clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
    for (const client of clientList) {
      if (client.url === url && 'focus' in client) return client.focus();
    }
    if (clients.openWindow) return clients.openWindow(url);
  }));
});
