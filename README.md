# 安卓虚拟定位 APP

基于 Android 调试 API（Mock Location）实现的虚拟定位工具，**无需 Root 权限**。

## 功能特点

- 🛰 无需 Root，利用系统 Mock Location 接口
- 📍 输入任意经纬度，一键修改手机 GPS 定位
- 🏙 内置北京/上海/广州/深圳快捷城市
- 🔄 后台持续运行（前台服务），不会被系统杀死
- 📡 同时欺骗 GPS 和 NETWORK 两个定位提供者

## 使用前准备

### 第一步：开启开发者选项
1. 进入手机「设置 → 关于手机」
2. 连续点击「版本号」7 次，开启「开发者选项」

### 第二步：设置模拟位置应用
1. 进入「设置 → 开发者选项」
2. 找到「模拟位置信息」或「选择模拟位置信息应用」
3. **选择本应用「虚拟定位」**

### 第三步：使用 APP
1. 打开 APP，输入目标经纬度（或点击城市快捷按钮）
2. 点击「▶ 开启虚拟定位」
3. 打开其他 APP（如地图），查看定位是否已更改
4. 使用完毕后点击「⬛ 停止虚拟定位」

## 编译方法

### 环境要求
- Android Studio Hedgehog 或更新版本
- JDK 17+
- Android SDK 34

### 步骤
1. 打开 Android Studio
2. 选择「Open」，打开本目录（AndroidVirtualLocation）
3. 等待 Gradle 同步完成
4. 连接手机 / 启动模拟器
5. 点击「Run」按钮编译并安装

## 项目结构

```
AndroidVirtualLocation/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml        # 权限声明
│       ├── java/com/example/.../
│       │   ├── MainActivity.kt        # 主界面逻辑
│       │   └── MockLocationService.kt # 核心：后台模拟定位服务
│       └── res/
│           ├── layout/activity_main.xml  # 界面布局
│           ├── values/                   # 字符串/颜色/主题
│           └── drawable/                 # 图标/形状资源
├── build.gradle        # 根 Gradle 配置
├── settings.gradle     # 项目设置
└── gradle.properties   # Gradle 属性
```

## 技术原理

Android 提供了 `LocationManager.addTestProvider()` 接口，允许被设置为"模拟位置应用"的 APP 向系统注入虚假 GPS 数据。本应用通过以下步骤实现虚拟定位：

1. 注册 GPS 和 NETWORK 两个测试 Provider
2. 每秒调用 `setTestProviderLocation()` 推送虚假坐标
3. 系统将该虚假坐标分发给所有监听位置的 APP

## 注意事项

- 部分应用（如微信、王者荣耀等）有额外的防作弊机制，可能无法被欺骗
- 请勿用于违法行为，后果自负
- Android 12+ 系统需要在 Manifest 中声明 `foregroundServiceType="location"`

## 参考项目

- [GoGoGo](https://github.com/ZCShou/GoGoGo) - 10.4k stars，Java 版虚拟定位
- [fake-gps](https://github.com/fe26w56/fake-gps) - Python 脚本通过 adb 模拟定位
