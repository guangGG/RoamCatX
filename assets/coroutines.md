# kotlin协程用法小结

[Kotlin协程源码地址](https://github.com/Kotlin/kotlinx.coroutines)

## 协程和线程的关系
线程是系统调度的最小单元，kotlin协程作用相当于将所有要处理的任务分成很多个小任务，然后将这些小任务按照需要分配给对应的主线程、空闲的IO线程、指定线程等去做处理，可以理解成安卓中Handler的post或postDelayed将任务加入到对应线程的任务队列中，而协程提供了更优秀的任务调度机制，协程一般情况下比直接使用线程具有更高的执行效率。

## CoroutineScope(协程作用范围)
通过CoroutineScope(context: CoroutineContext)创建CoroutineScope；
协程库中提供了4种CoroutineContext，也可以使用指定线程的Context：
* Dispatchers.Default 协程会在一个后台线程池中的一个线程中启动
* Dispatchers.IO 被设计为IO线程池，但实际上和Default共用了一个线程池
* Dispatchers.Unconfined 一个试验中的api，它的效果是启动的协程会在当前线程启动，但是在被suspend函数挂起后再继续后就会切换到suspend函数所在的线程
* Dispatchers.Main 代表协程会在与UI交互的主线程中启动，对于android项目来说，需要引进kotlinx-coroutines-android这个模块，否则直接使用会报IllegalStateException异常
* newSingleThreadContext(name: String) 新开启一个单独的线程处理协程任务

常用的CoroutineScope及写法：
```kotlin
// 主线程中执行CoroutineScope(下面两种写法等价)
MainScope()
CoroutineScope(Dispatchers.Main)
// 默认工作线程中执行CoroutineScope(下面两种写法等价)
GlobalScope
CoroutineScope(Dispatchers.Default)
```

## 开启协程任务
通过CoroutineScope的launch/async等方法创建并开启协程任务(可以通过CoroutineStart.LAZY参数设置job懒启动，调用了job.start或者job.join才会启动)
```kotlin
// 开启一个主线程中执行的协程任务，相当于Handler(Looper.getMainLooper()).post { $任务逻辑 }
val job = MainScope().launch { $任务逻辑 }
// 开启一个工作线程中执行的协程任务，相当于Handler( $工作线程Looper ).post { $任务逻辑 }
val job = GlobalScope.launch { $任务逻辑 }
// 延迟处理任务，相当于Handler(*).postDelayed({ $任务逻辑 }, time)
val job = CoroutineScope(*).launch {
	delay(time) //delay()为挂起函数，不会阻塞线程，在MainScope()中不会阻塞主线程UI操作
	$任务逻辑
}
// 开启一个带返回值的协程任务
val deferredJob = CoroutineScope(*).async { $任务逻辑及返回值 }
// 在协程任务中可以开启子任务，子任务的await()方法可以阻塞父任务，如：
GlobalScope.launch {
	async { $任务逻辑 }.await()
	// withContext(CoroutineContext){}等价于async(CoroutineContext){}.await()
	withContext(Dispatchers.Default) { $任务逻辑 }
}
```
Job是一个具有生命周期有父子结构可取消的任务对象，在job中启动的job称为子job，父job会等待所有子job执行完毕后才结束，cancel父job会同时取消所有的同作用域子job。job具有一系列的状态值:
```kotlin
  | **State**                        | [isActive] | [isCompleted] | [isCancelled] |
  | -------------------------------- | ---------- | ------------- | ------------- |
  | _New_ (optional initial state)   | `false`    | `false`       | `false`       |
  | _Active_ (default initial state) | `true`     | `false`       | `false`       |
  | _Completing_ (transient state)   | `true`     | `false`       | `false`       |
  | _Cancelling_ (transient state)   | `false`    | `false`       | `true`        |
  | _Cancelled_ (final state)        | `false`    | `true`        | `true`        |
  | _Completed_ (final state)        | `false`    | `true`        | `false`       |
```
job生命周期图如下:
```kotlin
/**
 * 
 *                                       wait children
 * +-----+ start  +--------+ complete   +-------------+  finish  +-----------+
 * | New | -----> | Active | ---------> | Completing  | -------> | Completed |
 * +-----+        +--------+            +-------------+          +-----------+
 *                  |  cancel / fail       |
 *                  |     +----------------+
 *                  |     |
 *                  V     V
 *              +------------+                           finish  +-----------+
 *              | Cancelling | --------------------------------> | Cancelled |
 *              +------------+                                   +-----------+
 * 
*/
```

##### 一些参考资料
[kotlin协程指北](http://yl.ehorizon.top/2019/02/01/kotlin%E5%8D%8F%E7%A8%8B%E6%8C%87%E5%8C%97/)
[Kotlin协程笔记](https://www.jianshu.com/p/8dc8abca50e3)
