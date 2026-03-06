import { create } from 'zustand'

// 服务状态管理
export const useServiceStore = create((set, get) => ({
    services: {},
    loading: {},
    
    setServices: (services) => set({ services }),
    
    updateServiceStatus: (id, status) => set((state) => ({
      services: { ...state.services, [id]: status }
    })),
    
    setLoading: (id, isLoading) => set((state) => ({
      loading: { ...state.loading, [id]: isLoading }
    })),
    
    fetchServices: async () => {
      try {
        const res = await fetch('/api/services/status')
        const data = await res.json()
        if (data.success) {
          set({ services: data.data })
        }
      } catch (error) {
        console.error('获取服务状态失败:', error)
      }
    }
  }))

// 日志状态管理
export const useLogStore = create((set, get) => ({
  serviceLogs: '',
  buildLogs: '',
  logLevel: 'all', // all | error | warn | info
  searchTerm: '',
  
  appendServiceLog: (message) => set((state) => ({
    serviceLogs: limitLogs(state.serviceLogs + message)
  })),
  
  appendBuildLog: (message) => set((state) => ({
    buildLogs: limitLogs(state.buildLogs + message)
  })),
  
  clearServiceLogs: () => set({ serviceLogs: '' }),
  clearBuildLogs: () => set({ buildLogs: '' }),
  
  setLogLevel: (level) => set({ logLevel: level }),
  setSearchTerm: (term) => set({ searchTerm: term }),
  
  // 获取过滤后的日志
  getFilteredServiceLogs: () => filterLogs(get().serviceLogs, get().logLevel, get().searchTerm),
  getFilteredBuildLogs: () => filterLogs(get().buildLogs, get().logLevel, get().searchTerm)
}))

// 构建状态管理
export const useBuildStore = create((set, get) => ({
  activeBuilds: [],
  buildHistory: [],
  currentBuild: null,
  buildProgress: 0,
  
  setActiveBuilds: (builds) => set({ activeBuilds: builds }),
  setBuildHistory: (history) => set({ buildHistory: history }),
  
  addActiveBuild: (build) => set((state) => ({
    activeBuilds: [...state.activeBuilds, build]
  })),
  
  updateBuildProgress: (buildId, progress) => set((state) => ({
    activeBuilds: state.activeBuilds.map(b =>
      b.id === buildId ? { ...b, ...progress } : b
    ),
    currentBuild: state.currentBuild?.id === buildId 
      ? { ...state.currentBuild, ...progress }
      : state.currentBuild
  })),
  
  removeActiveBuild: (buildId) => set((state) => ({
    activeBuilds: state.activeBuilds.filter(b => b.id !== buildId)
  })),
  
  setCurrentBuild: (build) => set({ currentBuild: build }),
  
  fetchActiveBuilds: async () => {
    try {
      const res = await fetch('/api/progress/active')
      const data = await res.json()
      if (data.success) {
        set({ activeBuilds: data.data })
      }
    } catch (error) {
      console.error('获取构建任务失败:', error)
    }
  },
  
  fetchBuildHistory: async (limit = 10) => {
    try {
      const res = await fetch(`/api/progress/history/recent?limit=${limit}`)
      const data = await res.json()
      if (data.success) {
        set({ buildHistory: data.data })
      }
    } catch (error) {
      console.error('获取构建历史失败:', error)
    }
  },
  
  cancelBuild: async (buildId) => {
    try {
      const res = await fetch(`/api/progress/${buildId}/cancel`, { method: 'POST' })
      const data = await res.json()
      if (data.success) {
        get().removeActiveBuild(buildId)
      }
      return data.success
    } catch (error) {
      console.error('取消构建失败:', error)
      return false
    }
  }
}))

// WebSocket 连接状态
export const useWebSocketStore = create((set, get) => ({
  connected: false,
  clientId: null,
  reconnectAttempts: 0,
  
  setConnected: (connected) => set({ connected }),
  setClientId: (clientId) => set({ clientId }),
  incrementReconnect: () => set((state) => ({ 
    reconnectAttempts: state.reconnectAttempts + 1 
  })),
  resetReconnect: () => set({ reconnectAttempts: 0 })
}))

// 辅助函数
function limitLogs(logs, maxLines = 1000) {
  const lines = logs.split('\n')
  if (lines.length > maxLines) {
    return lines.slice(-maxLines).join('\n')
  }
  return logs
}

function filterLogs(logs, level, searchTerm) {
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
