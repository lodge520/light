import { computed, onBeforeUnmount, onMounted, ref, unref, watch, type ComputedRef, type Ref } from 'vue'

type MessageHandler = (data: any) => void
type UrlSource = Ref<string> | ComputedRef<string> | (() => string)

function resolveUrl(source: UrlSource): string {
  return typeof source === 'function' ? source() : unref(source)
}

export function useWebSocket(urlSource: UrlSource, onMessage?: MessageHandler) {
  const socket = ref<WebSocket | null>(null)
  const connected = ref(false)
  const lastMessage = ref<any>(null)

  let reconnectTimer: number | null = null
  let manualClose = false

  function clearReconnectTimer() {
    if (reconnectTimer !== null) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function cleanupSocket() {
    if (!socket.value) return
    socket.value.onopen = null
    socket.value.onmessage = null
    socket.value.onerror = null
    socket.value.onclose = null
    socket.value.close()
    socket.value = null
  }

  function connect() {
    const url = resolveUrl(urlSource)
    if (!url) return

    clearReconnectTimer()
    cleanupSocket()

    manualClose = false

    const ws = new WebSocket(url)
    socket.value = ws

    ws.onopen = () => {
      connected.value = true
      console.log('WS connected:', url)
    }

    ws.onmessage = (event) => {
      if (typeof event.data !== 'string') return

      try {
        const parsed = JSON.parse(event.data)
        lastMessage.value = parsed
        onMessage?.(parsed)
      } catch (error) {
        console.warn('WS parse failed:', error)
      }
    }

    ws.onerror = (error) => {
      console.error('WS error:', error)
    }

    ws.onclose = () => {
      connected.value = false
      socket.value = null

      if (manualClose) return

      clearReconnectTimer()
      reconnectTimer = window.setTimeout(() => {
        connect()
      }, 3000)
    }
  }

  function reconnect() {
    manualClose = true
    clearReconnectTimer()
    cleanupSocket()
    connected.value = false
    connect()
  }

  function close() {
    manualClose = true
    clearReconnectTimer()
    cleanupSocket()
    connected.value = false
  }

  function send(payload: string | Record<string, unknown>) {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      return false
    }

    socket.value.send(typeof payload === 'string' ? payload : JSON.stringify(payload))
    return true
  }

  onMounted(() => {
    connect()
  })

  onBeforeUnmount(() => {
    close()
  })

  watch(
    () => resolveUrl(urlSource),
    (newUrl, oldUrl) => {
      if (!newUrl || !oldUrl || newUrl === oldUrl) return
      reconnect()
    },
  )

  return {
    socket,
    connected,
    lastMessage,
    connect,
    reconnect,
    close,
    send,
  }
}