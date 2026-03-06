import { useEffect, useRef } from 'react'
import { useWebSocketStore, useLogStore, useBuildStore, useServiceStore } from '../store/useAppStore'

const WS_URL = `ws://${window.location.host}/ws`
const MAX_RECONNECT_ATTEMPTS = 5
const RECONNECT_DELAY = 3000

export function useWebSocket() {
  // 使用 ref 存储连接状态，避免 React 严格模式的双重调用问题
  const wsRef = useRef(null)
  const reconnectTimerRef = useRef(null)
  const heartbeatTimerRef = useRef(null)
  const reconnectCountRef = useRef(0)
  const isConnectingRef = useRef(false)
  const isUnmountedRef = useRef(false)
  
  // 从 store 获取状态和 action
  const { 
    setConnected, 
    setClientId,
    incrementReconnect,
    resetReconnect 
  } = useWebSocketStore()
  
  const appendServiceLog = useLogStore(state => state.appendServiceLog)
  const appendBuildLog = useLogStore(state => state.appendBuildLog)
  const updateBuildProgress = useBuildStore(state => state.updateBuildProgress)
  const fetchServices = useServiceStore(state => state.fetchServices)

  useEffect(() => {
    isUnmountedRef.current = false
    
    const connect = () => {
      // 防止重复连接
      if (isConnectingRef.current) return
      if (wsRef.current?.readyState === WebSocket.OPEN) return
      if (wsRef.current?.readyState === WebSocket.CONNECTING) return
      
      isConnectingRef.current = true
      console.log('Connecting to WebSocket...')
      
      const socket = new WebSocket(WS_URL)
      wsRef.current = socket

      socket.onopen = () => {
        if (isUnmountedRef.current) {
          socket.close()
          return
        }
        
        console.log('WebSocket connected')
        isConnectingRef.current = false
        setConnected(true)
        reconnectCountRef.current = 0
        resetReconnect()
        
        // 订阅频道
        socket.send(JSON.stringify({
          type: 'subscribe',
          channels: ['logs:service', 'logs:build', 'build:progress', '*']
        }))
        
        // 启动心跳
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
          
          // 处理消息
          switch (data.type) {
            case 'connected':
              setClientId(data.clientId)
              break
              
            case 'message':
              // 处理频道消息
              switch (data.channel) {
                case 'logs:service':
                  appendServiceLog(data.data.message)
                  break
                case 'logs:build':
                  appendBuildLog(data.data.message)
                  break
                case 'build:progress':
                  updateBuildProgress(data.data.buildId, data.data)
                  // 构建完成时刷新服务状态
                  if (data.data.status === 'success' || data.data.status === 'failed') {
                    setTimeout(fetchServices, 2000)
                  }
                  break
              }
              break
              
            case 'pong':
              // 心跳响应，无需处理
              break
          }
        } catch (e) {
          console.error('WebSocket message parse error:', e)
        }
      }

      socket.onclose = () => {
        console.log('WebSocket closed')
        setConnected(false)
        isConnectingRef.current = false
        
        if (heartbeatTimerRef.current) {
          clearInterval(heartbeatTimerRef.current)
          heartbeatTimerRef.current = null
        }
        
        // 如果是组件已卸载，不重连
        if (isUnmountedRef.current) return
        
        // 尝试重连
        if (reconnectCountRef.current < MAX_RECONNECT_ATTEMPTS) {
          reconnectCountRef.current++
          incrementReconnect()
          reconnectTimerRef.current = setTimeout(() => {
            if (!isUnmountedRef.current) {
              connect()
            }
          }, RECONNECT_DELAY)
        }
      }

      socket.onerror = (error) => {
        console.error('WebSocket error:', error)
        isConnectingRef.current = false
      }
    }

    const disconnect = () => {
      isUnmountedRef.current = true
      
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current)
        reconnectTimerRef.current = null
      }
      if (heartbeatTimerRef.current) {
        clearInterval(heartbeatTimerRef.current)
        heartbeatTimerRef.current = null
      }
      if (wsRef.current) {
        // 移除事件监听，防止触发重连
        wsRef.current.onclose = null
        wsRef.current.close()
        wsRef.current = null
      }
      isConnectingRef.current = false
    }

    connect()
    return () => disconnect()
  }, [
    setConnected, setClientId, incrementReconnect, resetReconnect,
    appendServiceLog, appendBuildLog, updateBuildProgress, fetchServices
  ])

  // 获取 store 中的状态用于返回
  const { connected, clientId, reconnectAttempts } = useWebSocketStore()

  const sendMessage = (message) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message))
    }
  }

  return {
    connected,
    clientId,
    reconnectAttempts,
    sendMessage
  }
}

// 发送构建取消命令
export function useCancelBuild() {
  const { sendMessage } = useWebSocket()
  
  return (buildId) => {
    sendMessage({
      type: 'cancelBuild',
      buildId
    })
  }
}
