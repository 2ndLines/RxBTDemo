# RxBluetooth 


## 说明
响应式的传统蓝牙。
功能如下：
* 蓝牙搜索
* 订阅蓝牙连接状态
* 订阅扫描到的设备
* 蓝牙读写

## 方法介绍

### register()

    注册蓝牙广播
    
### unregister()
    
    注销蓝牙广播
    
###  asServer()

     把蓝牙作为服务端，接受其他蓝牙连接
     
### connect(String)

    作为客户端通过蓝牙地址去连接服务端
    
### subscribeAction()

    订阅蓝牙广播的action，如连接状态等
    
### subscribeFoundDevice()
    
    订阅扫描到的蓝牙设备
    
### subscribeFeedback()
    
    订阅接收到的数据
    
    