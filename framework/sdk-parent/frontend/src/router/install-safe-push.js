const SAFE_PUSH_INSTALLED_KEY = '__MS_SAFE_PUSH_INSTALLED__'

export function installSafeRouterPush(RouterCtor) {
  if (!RouterCtor || !RouterCtor.prototype || RouterCtor.prototype[SAFE_PUSH_INSTALLED_KEY]) {
    return
  }

  const routerPush = RouterCtor.prototype.push
  RouterCtor.prototype.push = function push(location, onResolve, onReject) {
    if (onResolve || onReject) {
      return routerPush.call(this, location, onResolve, onReject)
    }
    return routerPush.call(this, location).catch(error => error)
  }

  RouterCtor.prototype[SAFE_PUSH_INSTALLED_KEY] = true
}
