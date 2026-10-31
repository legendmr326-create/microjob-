const CACHE_NAME = 'taskflex-shell-v3';
const SHELL = ['/', '/manifest.webmanifest', '/icons/icon-192.png', '/icons/icon-512.png'];
const NAVIGATION_TIMEOUT_MS = 8000;
const PRECACHE_TIMEOUT_MS = 12000;

async function fetchWithTimeout(request, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(request, { signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    // A slow or missing icon/manifest should never prevent the worker activating.
    await Promise.allSettled(SHELL.map(async (url) => {
      try {
        const response = await fetchWithTimeout(url, PRECACHE_TIMEOUT_MS);
        if (response.ok) await cache.put(url, response);
      } catch (_) {}
    }));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    const keys = await caches.keys();
    // Preserve the existing offline home page when upgrading the cache version.
    for (const key of keys.filter((name) => name.startsWith('taskflex-shell-') && name !== CACHE_NAME)) {
      try {
        const oldCache = await caches.open(key);
        for (const request of await oldCache.keys()) {
          const response = await oldCache.match(request);
          if (response && SHELL.includes(new URL(request.url).pathname) && !(await cache.match(request))) {
            await cache.put(request, response);
          }
        }
      } catch (_) {}
    }
    await Promise.all(keys
      .filter((key) => key.startsWith('taskflex-shell-') && key !== CACHE_NAME)
      .map((key) => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);
  if (request.method !== 'GET' || url.origin !== self.location.origin) return;

  if (request.mode === 'navigate') {
    event.respondWith((async () => {
      // Start the request, but do not leave mobile visitors waiting indefinitely.
      const networkResponse = fetch(request).then(async (response) => {
        if (response.ok) {
          const cache = await caches.open(CACHE_NAME);
          // Do not hold the navigation response open for slow storage writes.
          event.waitUntil(cache.put('/', response.clone()).catch(() => {}));
        }
        return response;
      });
      const cachedPage = caches.match(request).then((cached) => cached || caches.match('/'));
      const timeoutFallback = new Promise((resolve) => {
        setTimeout(async () => {
          const cached = await cachedPage;
          // On a first visit there is no offline copy, so keep the live request
          // instead of aborting a slow but progressing cellular download.
          if (cached) resolve(cached);
        }, NAVIGATION_TIMEOUT_MS);
      });

      try {
        return await Promise.race([networkResponse, timeoutFallback]);
      } catch (_) {
        return (await cachedPage) || Response.error();
      }
    })());
    return;
  }

  const isShellAsset = SHELL.includes(url.pathname);
  if (isShellAsset) {
    event.respondWith((async () => {
      const cached = await caches.match(request);
      if (cached) return cached;
      try {
        const response = await fetchWithTimeout(request, PRECACHE_TIMEOUT_MS);
        if (response.ok) caches.open(CACHE_NAME).then((cache) => cache.put(request, response.clone()));
        return response;
      } catch (_) {
        return Response.error();
      }
    })());
  }
});
