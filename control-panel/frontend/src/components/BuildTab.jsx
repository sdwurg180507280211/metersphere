import { useEffect, useCallback } from 'react'
import { toast } from 'react-hot-toast'
import { useBuildStore } from '../store/useAppStore'
import LogViewer from './LogViewer'
import BuildProgress from './BuildProgress'
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
  const { activeBuilds, fetchActiveBuilds, addActiveBuild } = useBuildStore()

  useEffect(() => {
    fetchActiveBuilds()
  }, [fetchActiveBuilds])

  const isBuilding = activeBuilds.some(b => b.status === 'running')

  const handleBuild = useCallback(async (moduleId) => {
    if (isBuilding) {
      toast.error('已有构建任务进行中，请等待完成')
      return
    }

    const module = MODULES.find(m => m.id === moduleId)
    
    try {
      const res = await fetch('/api/build/frontend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ module: moduleId })
      })
      
      const data = await res.json()
      
      if (data.success) {
        toast.success(`已启动 ${module.name} 的构建`)
        // 临时添加到活跃构建列表，等待 WebSocket 更新
        if (data.buildId) {
          addActiveBuild({
            id: data.buildId,
            module: module.name,
            status: 'running',
            currentStep: 0,
            totalSteps: 5,
            overallProgress: 0,
            stepName: '准备环境'
          })
        }
      } else {
        toast.error(data.error || '启动构建失败')
      }
    } catch (error) {
      toast.error(`网络错误: ${error.message}`)
    }
  }, [isBuilding, addActiveBuild])

  return (
    <div className="tab-content">
      <BuildProgress />
      
      <div className="card">
        <div className="card-header">
          <h2 className="card-title">前端构建</h2>
          {isBuilding && <span className="building-badge">构建中...</span>}
        </div>
        <div className="btn-grid">
          {MODULES.map(module => (
            <button
              key={module.id}
              className="btn-build"
              onClick={() => handleBuild(module.id)}
              disabled={isBuilding}
            >
              {module.name}
            </button>
          ))}
        </div>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0 }}>
        <div className="card-header">
          <h2 className="card-title">构建日志</h2>
        </div>
        <LogViewer type="build" />
      </div>
    </div>
  )
}

export default BuildTab
