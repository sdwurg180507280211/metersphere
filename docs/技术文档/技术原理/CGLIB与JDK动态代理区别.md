# CGLIB 与 JDK 动态代理区别

## 概述

在 Java 中，动态代理是实现 AOP（面向切面编程）的核心技术。Spring AOP 支持两种代理方式：
- **JDK 动态代理**：基于接口的代理
- **CGLIB 代理**：基于继承的代理

## 一、JDK 动态代理

### 1.1 基本原理

JDK 动态代理基于 **Java 反射机制**，通过 `java.lang.reflect.Proxy` 类和 `InvocationHandler` 接口实现。

### 1.2 实现机制

- 使用 `Proxy.newProxyInstance()` 创建代理对象
- 代理对象实现目标对象的所有接口
- 通过 `InvocationHandler.invoke()` 方法拦截方法调用

### 1.3 代码示例

```java
// 接口
public interface UserService {
    void save();
}

// 实现类
public class UserServiceImpl implements UserService {
    @Override
    public void save() {
        System.out.println("保存用户");
    }
}

// 代理处理器
public class MyInvocationHandler implements InvocationHandler {
    private Object target;
    
    public MyInvocationHandler(Object target) {
        this.target = target;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("方法执行前");
        Object result = method.invoke(target, args);
        System.out.println("方法执行后");
        return result;
    }
}

// 使用
UserService target = new UserServiceImpl();
InvocationHandler handler = new MyInvocationHandler(target);
UserService proxy = (UserService) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    handler
);
proxy.save();
```

### 1.4 特点

- ✅ **优点**：
  - JDK 原生支持，无需额外依赖
  - 性能相对稳定
  - 代理对象类型为接口类型

- ❌ **缺点**：
  - **只能代理实现了接口的类**
  - 如果目标类没有实现接口，无法使用 JDK 动态代理
  - 通过反射调用方法，性能略低于 CGLIB

## 二、CGLIB 动态代理

### 2.1 基本原理

CGLIB（Code Generation Library）是一个**代码生成库**，通过**字节码技术**在运行时动态生成目标类的子类。

### 2.2 实现机制

- 使用 ASM 框架操作字节码
- 生成目标类的子类作为代理类
- 通过方法拦截器（MethodInterceptor）拦截方法调用
- 子类重写父类方法，在方法中调用拦截器

### 2.3 代码示例

```java
// 目标类（不需要实现接口）
public class UserService {
    public void save() {
        System.out.println("保存用户");
    }
}

// 方法拦截器
public class MyMethodInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("方法执行前");
        Object result = proxy.invokeSuper(obj, args);
        System.out.println("方法执行后");
        return result;
    }
}

// 使用
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserService.class);
enhancer.setCallback(new MyMethodInterceptor());
UserService proxy = (UserService) enhancer.create();
proxy.save();
```

### 2.4 特点

- ✅ **优点**：
  - **可以代理没有实现接口的类**
  - 通过方法代理（MethodProxy）调用，性能通常优于 JDK 动态代理
  - 生成的代理类是目标类的子类

- ❌ **缺点**：
  - 需要引入 CGLIB 依赖
  - **不能代理 final 类和方法**（因为需要继承）
  - **不能代理 private 方法**（子类无法访问）
  - 生成的代理类会占用更多内存

## 三、核心区别对比

| 对比项 | JDK 动态代理 | CGLIB 代理 |
|--------|-------------|-----------|
| **实现原理** | 基于接口，使用反射 | 基于继承，使用字节码技术 |
| **代理对象类型** | 接口类型 | 目标类的子类 |
| **目标类要求** | 必须实现接口 | 不需要实现接口 |
| **final 类/方法** | 可以代理（通过接口） | 不能代理 |
| **private 方法** | 可以代理（通过接口） | 不能代理 |
| **性能** | 反射调用，性能中等 | 方法代理调用，性能较好 |
| **依赖** | JDK 原生支持 | 需要 CGLIB 库 |
| **内存占用** | 较小 | 较大（生成子类） |

## 四、Spring AOP 中的选择策略

### 4.1 默认策略

Spring AOP 默认的代理选择策略：

1. **如果目标对象实现了接口** → 使用 **JDK 动态代理**
2. **如果目标对象没有实现接口** → 使用 **CGLIB 代理**

### 4.2 强制使用 CGLIB

可以通过配置强制使用 CGLIB 代理：

```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)  // 强制使用 CGLIB
public class AppConfig {
}
```

或者在 XML 配置中：

```xml
<aop:aspectj-autoproxy proxy-target-class="true"/>
```

或者在 `DefaultAdvisorAutoProxyCreator` 中设置：

```java
@Bean
public DefaultAdvisorAutoProxyCreator getDefaultAdvisorAutoProxyCreator() {
    DefaultAdvisorAutoProxyCreator daap = new DefaultAdvisorAutoProxyCreator();
    daap.setProxyTargetClass(true);  // 强制使用 CGLIB
    return daap;
}
```

**项目中的实际应用**：在 `ShiroConfig.java` 中配置了 `setProxyTargetClass(true)`，强制使用 CGLIB 代理。

### 4.3 为什么项目中使用 CGLIB？

查看项目配置：

```88:94:framework/sdk-parent/sdk/src/main/java/io/metersphere/autoconfigure/ShiroConfig.java
    @Bean
    @DependsOn({"lifecycleBeanPostProcessor"})
    public DefaultAdvisorAutoProxyCreator getDefaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator daap = new DefaultAdvisorAutoProxyCreator();
        daap.setProxyTargetClass(true);
        return daap;
    }
```

**可能的原因**：
1. **统一代理方式**：避免 JDK 动态代理和 CGLIB 混用带来的问题
2. **性能考虑**：CGLIB 在某些场景下性能更好
3. **功能需求**：某些类可能没有实现接口，需要统一使用 CGLIB
4. **类型转换**：CGLIB 代理的对象类型是目标类本身，类型转换更直观

## 五、性能对比

### 5.1 创建代理对象性能

- **JDK 动态代理**：创建速度快，但每次调用都需要反射
- **CGLIB 代理**：创建速度较慢（需要生成字节码），但调用速度快

### 5.2 方法调用性能

- **JDK 动态代理**：通过反射调用，性能中等
- **CGLIB 代理**：通过方法代理（MethodProxy）调用，性能更好

**注意**：性能差异在实际应用中通常不明显，除非是高频调用的场景。

## 六、使用建议

### 6.1 选择 JDK 动态代理的场景

- ✅ 目标类已经实现了接口
- ✅ 希望保持代码的接口导向设计
- ✅ 不需要代理 final 类或方法
- ✅ 希望减少依赖

### 6.2 选择 CGLIB 代理的场景

- ✅ 目标类没有实现接口
- ✅ 需要代理没有接口的第三方类
- ✅ 对性能有较高要求
- ✅ 需要统一代理方式（如 Spring 配置）

### 6.3 注意事项

1. **final 类和方法**：CGLIB 无法代理 final 类和方法，如果必须代理，需要修改目标类
2. **private 方法**：CGLIB 无法代理 private 方法，AOP 切面不会生效
3. **构造函数**：CGLIB 会调用目标类的无参构造函数，确保目标类有无参构造函数
4. **内存占用**：CGLIB 生成的代理类会占用更多内存

## 七、常见问题

### 7.1 为什么 Spring 默认优先使用 JDK 动态代理？

- JDK 动态代理是 JDK 原生支持，无需额外依赖
- 符合面向接口编程的设计原则
- 代理对象类型更清晰（接口类型）

### 7.2 什么时候会切换到 CGLIB？

- 目标类没有实现任何接口
- 配置了 `proxyTargetClass = true`

### 7.3 如何判断当前使用的是哪种代理？

```java
// 判断代理类型
if (bean instanceof SpringProxy) {
    if (AopUtils.isJdkDynamicProxy(bean)) {
        System.out.println("使用 JDK 动态代理");
    } else if (AopUtils.isCglibProxy(bean)) {
        System.out.println("使用 CGLIB 代理");
    }
}
```

### 7.4 事务失效问题

**问题**：在同一个类中，方法 A 调用方法 B，方法 B 上的事务注解不生效。

**原因**：Spring AOP 是基于代理的，只有通过代理对象调用方法，AOP 才会生效。在同一个类中直接调用，不会经过代理。

**解决方案**：
1. 将方法 B 提取到另一个 Service 类中
2. 通过 `ApplicationContext` 获取代理对象调用
3. 使用 `@Transactional` 的 `propagation` 属性

## 八、总结

1. **JDK 动态代理**：基于接口，使用反射，适合有接口的场景
2. **CGLIB 代理**：基于继承，使用字节码，适合没有接口的场景
3. **Spring 默认策略**：有接口用 JDK，无接口用 CGLIB
4. **项目配置**：通过 `setProxyTargetClass(true)` 强制使用 CGLIB
5. **性能差异**：实际应用中差异不大，CGLIB 略优
6. **选择建议**：根据实际需求选择，统一配置更利于维护

## 参考资源

- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)
- [CGLIB 官方文档](https://github.com/cglib/cglib)
- [Java 动态代理详解](https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html)

