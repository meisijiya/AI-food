const DB_NAME = 'photo_cache'
const STORE_NAME = 'photos'
const DB_VERSION = 1
const CACHE_MAX_AGE = 7 * 24 * 60 * 60 * 1000 // 7 days
const BLOB_URL_MAP = new Map<string, string>()

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'url' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function getCached(url: string): Promise<{ blob: Blob; timestamp: number } | null> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const req = store.get(url)
    req.onsuccess = () => resolve(req.result || null)
    req.onerror = () => resolve(null)
  })
}

async function putCache(url: string, blob: Blob): Promise<void> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    store.put({ url, blob, timestamp: Date.now() })
    tx.oncomplete = () => resolve()
    tx.onerror = () => resolve()
  })
}

export async function getPhotoBlobUrl(url: string): Promise<string> {
  if (!url) return ''

  // Check in-memory blob URL map first
  const existing = BLOB_URL_MAP.get(url)
  if (existing) return existing

  // Check IndexedDB cache
  const cached = await getCached(url)
  if (cached && Date.now() - cached.timestamp < CACHE_MAX_AGE) {
    const blobUrl = URL.createObjectURL(cached.blob)
    BLOB_URL_MAP.set(url, blobUrl)
    return blobUrl
  }

  // Fetch from network
  try {
    const resp = await fetch(url)
    if (!resp.ok) return url // fallback to original URL
    const blob = await resp.blob()

    // Cache in IndexedDB
    await putCache(url, blob)

    // Create blob URL
    const blobUrl = URL.createObjectURL(blob)
    BLOB_URL_MAP.set(url, blobUrl)
    return blobUrl
  } catch {
    return url // fallback to original URL on error
  }
}

export async function prefetchPhotos(urls: string[]): Promise<Map<string, string>> {
  const result = new Map<string, string>()
  const toFetch: string[] = []

  for (const url of urls) {
    if (!url) continue
    const existing = BLOB_URL_MAP.get(url)
    if (existing) {
      result.set(url, existing)
      continue
    }
    const cached = await getCached(url)
    if (cached && Date.now() - cached.timestamp < CACHE_MAX_AGE) {
      const blobUrl = URL.createObjectURL(cached.blob)
      BLOB_URL_MAP.set(url, blobUrl)
      result.set(url, blobUrl)
    } else {
      toFetch.push(url)
    }
  }

  // Fetch uncached in parallel
  await Promise.allSettled(
    toFetch.map(async (url) => {
      try {
        const resp = await fetch(url)
        if (!resp.ok) {
          result.set(url, url)
          return
        }
        const blob = await resp.blob()
        await putCache(url, blob)
        const blobUrl = URL.createObjectURL(blob)
        BLOB_URL_MAP.set(url, blobUrl)
        result.set(url, blobUrl)
      } catch {
        result.set(url, url)
      }
    })
  )

  return result
}
