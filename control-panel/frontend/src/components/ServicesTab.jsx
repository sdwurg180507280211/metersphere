import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-hot-toast'
import LogViewer from './LogViewer'
import './ServicesTab.css'

const SERVICES = [
  { id: 'eureka', name: 'Eureka' },
  { id: 'gateway', name: 'Gateway' },
  { id: 'system-setting', name: 'System Setting' },
  { id: 'project-management', name: 'Project Management' },
  { id: 'test-track', name: 'Test Track' },
  { id: 'api-test', name: 'API Test' },
  { id: 'performance-test', name: 'Performance Test' },
  { id: 'report-stat', name: 'Report Stat' },
  { id: 'workstation', name: 'Workstation' },
  { id: 'workflow-service', name: 'Workflow Service' },
  { id: 'analytics-stat', name: 'Analytics Stat' }
]

function ServicesTab() {
  const [logs, setLogs] = useState('')
  const [serviceStatus, setServiceStatus] = useState({})
  const [loading, setLoading] = useState({})

  // 获取服务状态
  const fetchStatus = useCallback(async () => {
    try {
      const res = await fetch('/api/services/status')
      const data = await res.json()
      if (data.success) {
        setServiceStatus(data.data)
      }
    } catch (error) {
      console.error('Failed to fetch status:', error)
    }
  }, [])

  // 初始加载和定时刷新
  useEffect(() => {
    fetchStatus()
    const interval = setInterval(fetchStatus, 3000)
    return () => clearInterval(interval)
  }, [fetchStatus])

  // 连接 SSE 日志流
  useEffect(() => {
    const eventSource = new EventSource('/api/logs/stream')
    
    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'service' || data.type === 'system') {
          setLogs(prev => {
            const newLogs = prev + data.message
            const lines = newLogs.split('\n')
            if (lines.length > 1000) {
              return lines.slice(-1000).join('\n')
            }
            return newLogs
          })
        }
      } catch (e) {
        console.error('Parse error:', e)
      }
    }

    return () => {
      eventSource.close()
    }
  }, [])

  // 切换服务状态
  const toggleService = useCallback(async (serviceId) => {
    const isRunning = serviceStatus[serviceId]
    const action = isRunning ? '停止' : '启动'
    
    setLoading(prev => ({ ...prev, [serviceId]: true }))

    try {
      const endpoint = `/api/services/${serviceId}/${isRunning ? 'stop' : 'start'}`
      const res = await fetch(endpoint, { method: 'POST' })
      const data = await res.json()

      if (data.success) {
        toast.success(`${action}命令已发送`)
        // 延迟刷新状态
        setTimeout(fetchStatus, 2000)
      } else {
        toast.error(data.error || `${action}失败`)
      }
    } catch (error) {
      toast.error(`网络错误: ${error.message}`)
    } finally {
      setTimeout(() => {
        setLoading(prev => ({ ...prev, [serviceId]: false }))
      }, 2000)
    }
  }, [serviceStatus, fetchStatus])

  // 批量操作
  const handleBatchAction = useCallback(async (action) => {
    const endpoint = action === 'start' ? '/api/services/start-all' : '/api/services/stop-all'
    
    toast.promise(
      fetch(endpoint, { method: 'POST' }),
      {
        loading: `正在${action === 'start' ? '启动' : '停止'}所有服务...`,
        success: '命令已发送',
        error: '操作失败'
      }
    )
    
    setTimeout(fetchStatus, 5000)
  }, [fetchStatus])

  const clearLogs = useCallback(() => {
    setLogs('')
  }, [])

  return (
    <div className="tab-content">
      <div className="card">
        <div className="card-header">
          <h2 className="card-title">服务管理</h2>
          <div className="batch-actions">
            <button className="btn-batch btn-start" onClick={() => handleBatchAction('start')}>
              启动全部
            </button>
            <button className="btn-batch btn-stop" onClick={() => handleBatchAction('stop')}>
              停止全部
            </button>
          </div>
        </div>
        <div className="btn-grid">
          {SERVICES.map(service => {
            const isRunning = serviceStatus[service.id]
            const isLoading = loading[service.id]
            
            return (
              <button
                key={service.id}
                className={`btn-service ${isRunning ? 'running' : 'stopped'}`}
                onClick={() => toggleService(service.id)}
                disabled={isLoading}
              >
                {isLoading ? (
                  <><span className="loading"></span></>
                ) : (
                  <>
                    <span className="status-dot"></span>
                    {service.name}
                  </>
                )}
              </button>
            )
          })}
        </div>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0 }}>
        <div className="card-header">
          <h2 className="card-title">服务日志</h2>
          <button className="btn-secondary btn-small" onClick={clearLogs}>
            清空
          </button>
        </div>
        <LogViewer logs={logs} type="service" />
      </div>
    </div>
  )
}

export default ServicesTab
