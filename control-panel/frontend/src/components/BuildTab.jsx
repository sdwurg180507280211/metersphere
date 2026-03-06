import { useState, useEffect, useRef, useCallback } from 'react'
import { toast } from 'react-hot-toast'
import LogViewer from './LogViewer'
import './BuildTab.css'

const MODULES = [
  { id: 'system-setting', name: 'System Setting' },
  { id: 'project-management', name: 'Project Management' },
  { id: 'test-track', name: 'Test Track' },
  { id: 'api-test', name: 'API Test' },
  { id: 'performance-test', name: 'Performance Test' },
  { id: 'report-stat', name: 'Report Stat' },
  { id: 'workstation', name: 'Workstation' },
  { id: 'analytics-stat', name: 'Analytics Stat' },
  { id: 'sdk-parent', name: 'SDK Parent (Gateway)' }
]

function BuildTab() {
  const [logs, setLogs] = useState('')
  const [building, setBuilding] = useState(null)
  const logViewerRef = useRef(null)

  // 连接 SSE 日志流
  useEffect(() => {
    const eventSource = new EventSource('/api/logs/stream')
    
    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'build' || data.type === 'system') {
          setLogs(prev => {
            const newLogs = prev + data.message
            // 限制日志行数
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

    eventSource.onerror = () => {
      console.log('SSE connection error')
    }

    return () => {
      eventSource.close()
    }
  }, [])

  const handleBuild = useCallback(async (moduleId) => {
    if (building) {
      toast.error('已有构建任务进行中')
      return
    }

    const module = MODULES.find(m => m.id === moduleId)
    toast.promise(
      fetch('/api/build/frontend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ module: moduleId })
      }),
      {
        loading: `正在启动 ${module.name} 的构建...`,
        success: '构建任务已启动',
        error: '启动构建失败'
      }
    )

    setBuilding(moduleId)
    setTimeout(() => setBuilding(null), 5000)
  }, [building])

  const clearLogs = useCallback(() => {
    setLogs('')
  }, [])

  return (
    <div className="tab-content">
      <div className="card">
        <div className="card-header">
          <h2 className="card-title">前端构建</h2>
        </div>
        <div className="btn-grid">
          {MODULES.map(module => (
            <button
              key={module.id}
              className="btn-build"
              onClick={() => handleBuild(module.id)}
              disabled={building === module.id}
            >
              {building === module.id ? (
                <><span className="loading"></span> 构建中...</>
              ) : (
                module.name
              )}
            </button>
          ))}
        </div>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0 }}>
        <div className="card-header">
          <h2 className="card-title">构建日志</h2>
          <button className="btn-secondary btn-small" onClick={clearLogs}>
            清空
          </button>
        </div>
        <LogViewer logs={logs} type="build" />
      </div>
    </div>
  )
}

export default BuildTab
