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
      await navigator.serviceWorker.register('/sw.js', { scope: '/' });
      console.log('Game Arena service worker registered.');
    } catch (error) {
      console.error('Service worker registration failed:', error);
    }
  });
}
