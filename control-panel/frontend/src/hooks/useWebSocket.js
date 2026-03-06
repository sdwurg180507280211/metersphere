import { useEffect, useRef } from 'react'
import { useWebSocketStore, useLogStore, useBuildStore, useServiceStore } from '../store/useAppStore'

const WS_URL = `ws://${window.location.host}/ws`
const MAX_RECONNECT_ATTEMPTS = 5
const RECONNECT_DELAY = 3000

export function useWebSocket() {
  const wsRef = useRef(null)
  const reconnectTimerRef = useRef(null)
  const heartbeatTimerRef = useRef(null)
  const reconnectCountRef = useRef(0)
  const isConnectingRef = useRef(false)
  const isUnmountedRef = useRef(false)
  const intentionalCloseRef = useRef(false)
  
  // 从 store 获取 action ref（避免依赖问题）
  const appendServiceLogRef = useRef(useLogStore.getState().appendServiceLog)
  const appendBuildLogRef = useRef(useLogStore.getState().appendBuildLog)
  const updateBuildProgressRef = useRef(useBuildStore.getState().updateBuildProgress)
  const fetchServicesRef = useRef(useServiceStore.getState().fetchServices)
  
  const { 
    setConnected, 
    setClientId,
    incrementReconnect,
    resetReconnect 
  } = useWebSocketStore()

  useEffect(() => {
    isUnmountedRef.current = false
    intentionalCloseRef.current = false
    
    const connect = () => {
      if (isConnectingRef.current) return
      if (wsRef.current?.readyState === WebSocket.OPEN) return
      if (wsRef.current?.readyState === WebSocket.CONNECTING) return
      
      isConnectingRef.current = true
      
      const socket = new WebSocket(WS_URL)
      wsRef.current = socket

      socket.onopen = () => {
        if (isUnmountedRef.current || intentionalCloseRef.current) {
          socket.close()
          return
        }
        
        isConnectingRef.current = false
        setConnected(true)
        reconnectCountRef.current = 0
        resetReconnect()
        
        socket.send(JSON.stringify({
          type: 'subscribe',
          channels: ['logs:service', 'logs:build', 'build:progress', '*']
        }))
        
        heartbeatTimerRef.current = setInterval(() => {
          if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({ type: 'ping' }))
          }
        }, 30000)
      }

      socket.onmessage = (event) => {
        if (isUnmountedRef.current) return
        
        try {
          const data = JSON.parse(event.data)
          
          switch (data.type) {
            case 'connected':
              setClientId(data.clientId)
              break
            case 'message':
              switch (data.channel) {
                case 'logs:service':
                  appendServiceLogRef.current(data.data.message)
                  break
                case 'logs:build':
                  appendBuildLogRef.current(data.data.message)
                  break
                case 'build:progress':
                  updateBuildProgressRef.current(data.data.buildId, data.data)
                  if (data.data.status === 'success' || data.data.status === 'failed') {
                    setTimeout(() => fetchServicesRef.current(), 2000)
                  }
                  break
              }
              break
          }
        } catch (e) {
          // 静默处理解析错误
        }
      }

      socket.onclose = () => {
        setConnected(false)
        isConnectingRef.current = false
        
        if (heartbeatTimerRef.current) {
          clearInterval(heartbeatTimerRef.current)
          heartbeatTimerRef.current = null
        }
        
        // 如果是组件卸载或主动关闭，不重连
        if (isUnmountedRef.current || intentionalCloseRef.current) return
        
        // 重连
        if (reconnectCountRef.current < MAX_RECONNECT_ATTEMPTS) {
          reconnectCountRef.current++
          incrementReconnect()
          reconnectTimerRef.current = setTimeout(() => {
            if (!isUnmountedRef.current && !intentionalCloseRef.current) {
              connect()
            }
          }, RECONNECT_DELAY)
        }
      }

      socket.onerror = () => {
        isConnectingRef.current = false
        // 静默处理错误，让 onclose 处理重连
      }
    }

    const disconnect = () => {
      isUnmountedRef.current = true
      intentionalCloseRef.current = true
      
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current)
        reconnectTimerRef.current = null
      }
      if (heartbeatTimerRef.current) {
        clearInterval(heartbeatTimerRef.current)
        heartbeatTimerRef.current = null
      }
      if (wsRef.current) {
        wsRef.current.onclose = null
        wsRef.current.onerror = null
        wsRef.current.close()
        wsRef.current = null
      }
      isConnectingRef.current = false
    }

    // 延迟连接，避免 React 严格模式的快速创建/销毁
    const timer = setTimeout(connect, 100)
    
    return () => {
      clearTimeout(timer)
      disconnect()
    }
  }, [setConnected, setClientId, incrementReconnect, resetReconnect])

  const { connected, clientId, reconnectAttempts } = useWebSocketStore()

  const sendMessage = (message) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message))
    }
  }

  return { connected, clientId, reconnectAttempts, sendMessage }
}

export function useCancelBuild() {
  const { sendMessage } = useWebSocket()
  return (buildId) => sendMessage({ type: 'cancelBuild', buildId })
}
