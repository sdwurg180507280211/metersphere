import { useRef, useEffect } from 'react'
import './LogViewer.css'

function LogViewer({ logs, type = 'service' }) {
  const logRef = useRef(null)
  const shouldAutoScroll = useRef(true)

  // 自动滚动到底部
  useEffect(() => {
    if (logRef.current && shouldAutoScroll.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [logs])

  // 处理滚动事件，判断是否应该自动滚动
  const handleScroll = () => {
    if (logRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = logRef.current
      // 如果用户滚动到底部附近，继续自动滚动
      shouldAutoScroll.current = scrollHeight - scrollTop - clientHeight < 50
    }
  }

  // 根据类型设置颜色主题
  const getThemeClass = () => {
    switch (type) {
      case 'build':
        return 'log-theme-build'
      case 'service':
      default:
        return 'log-theme-service'
    }
  }

  return (
    <div className={`log-container ${getThemeClass()}`}>
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
