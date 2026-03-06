import { useRef, useEffect, useState, useMemo } from 'react'
import { useLogStore } from '../store/useAppStore'
import './LogViewer.css'

function LogViewer({ type }) {
  const logRef = useRef(null)
  const shouldAutoScroll = useRef(true)
  
  const { 
    logLevel, 
    setLogLevel, 
    searchTerm, 
    setSearchTerm,
    serviceLogs,
    buildLogs,
    clearServiceLogs,
    clearBuildLogs
  } = useLogStore()
  
  // 直接从 store 获取原始日志
  const originalLogs = type === 'build' ? buildLogs : serviceLogs
  
  // 实时计算过滤后的日志
  const logs = useMemo(() => {
    return filterLogs(originalLogs, logLevel, searchTerm)
  }, [originalLogs, logLevel, searchTerm])

  // 自动滚动到底部
  useEffect(() => {
    if (logRef.current && shouldAutoScroll.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [logs])

  const handleScroll = () => {
    if (logRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = logRef.current
      shouldAutoScroll.current = scrollHeight - scrollTop - clientHeight < 50
    }
  }

  const handleClear = () => {
    if (type === 'build') {
      clearBuildLogs()
    } else {
      clearServiceLogs()
    }
  }

  const handleDownload = () => {
    const blob = new Blob([originalLogs], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${type}-logs-${new Date().toISOString().slice(0, 10)}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  const getThemeClass = () => {
    switch (type) {
      case 'build':
        return 'log-theme-build'
      case 'service':
      default:
        return 'log-theme-service'
    }
  }

  const matchCount = useMemo(() => {
    if (!searchTerm) return 0
    return logs.split('\n').filter(line => 
      line.toLowerCase().includes(searchTerm.toLowerCase())
    ).length
  }, [logs, searchTerm])

// 辅助函数：过滤日志
function filterLogs(logs, level, searchTerm) {
  if (!logs) return ''
  
  let lines = logs.split('\n')
  
  // 按级别过滤
  if (level !== 'all') {
    lines = lines.filter(line => {
      if (level === 'error') return line.includes('ERROR') || line.includes('✗') || line.includes('失败')
      if (level === 'warn') return line.includes('WARN') || line.includes('warning')
      if (level === 'info') return line.includes('INFO') || line.includes('[系统]')
      return true
    })
  }
  
  // 按搜索词过滤
  if (searchTerm) {
    const term = searchTerm.toLowerCase()
    lines = lines.filter(line => line.toLowerCase().includes(term))
  }
  
  return lines.join('\n')
}

  return (
    <div className={`log-container ${getThemeClass()}`}>
      <div className="log-toolbar">
        <div className="log-filters">
          <select 
            value={logLevel} 
            onChange={(e) => setLogLevel(e.target.value)}
            className="log-select"
          >
            <option value="all">全部级别</option>
            <option value="error">错误</option>
            <option value="warn">警告</option>
            <option value="info">信息</option>
          </select>
          
          <div className="log-search">
            <input
              type="text"
              placeholder="搜索日志..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="log-search-input"
            />
            {searchTerm && (
              <span className="match-count">
                {matchCount} 条匹配
              </span>
            )}
          </div>
        </div>
        
        <div className="log-actions">
          <button className="btn-icon" onClick={handleDownload} title="下载日志">
            💾
          </button>
          <button className="btn-icon" onClick={handleClear} title="清空日志">
            🗑️
          </button>
        </div>
      </div>
      
      <div 
        ref={logRef}
        className="log"
        onScroll={handleScroll}
      >
        {logs || <span className="log-placeholder">等待日志输出...</span>}
      </div>
    </div>
  )
}

export default LogViewer
