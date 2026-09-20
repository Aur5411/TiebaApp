<div align="center">

# 贴吧 Lite · TiebaApp

**一个第三方百度贴吧 Android 客户端**

基于 [TiebaLite](https://github.com/HuanCheng65/TiebaLite) 4.0-dev 分支二次开发，使用 Jetpack Compose 构建，回归 Material Design 3 视觉。

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](./LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg)
![minSdk](https://img.shields.io/badge/minSdk-23-orange.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-36-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF.svg)
![Compose](https://img.shields.io/badge/Compose-BOM%202026.05.01-4285F4.svg)

[下载最新版本](../../releases/latest) · [问题反馈](../../issues)

</div>

---

## 项目简介

**贴吧 Lite（TiebaApp）** 是一个**非官方**的百度贴吧移动客户端，专注于三件事：

1. **干净** —— 无开屏广告、无信息流广告、无推送骚扰；
2. **还原** —— 内容与功能对齐官方客户端，界面按 Material Design 3 重新设计；
3. **轻快** —— 纯 Compose 实现，启动快、滑动跟手。

> ⚠️ **声明**：本软件与百度公司无任何关联，为非官方第三方客户端。所有贴吧内容版权归百度及原作者所有。
> **本软件及源码仅供学习交流使用，严禁用于商业用途。**

---

## 功能一览

### 基础浏览

- **主页四栏** —— 推荐 / 关注 / 查人（未登录为两栏），顶栏一站式切换
- **吧列表与吧内浏览** —— 支持主题列表多种排序（回复时间 / 发布时间 / 精品），列表可切单列 / 双列
- **帖子详情** —— 楼层与楼中楼完整展示，支持只看楼主、倒序查看、楼层跳转
- **图片查看器** —— 大图浏览、缩放拖拽、长按保存
- **视频播放** —— 基于 AndroidX Media3 的站内视频播放
- **沉浸式滚动** —— 下滑自动收起搜索框，顶栏下拉收纳列表切换/排序入口

### 搜索与用户

- **搜索框历史** —— 自动记录搜索历史，附带「猜你想搜」
- **「查人」页（本分支新增）** —— 输入 8~10 位贴吧 UID（或直接粘贴分享文案，自动提取 `#数字#`），即可查看任意用户的资料卡：
  - 头像、昵称、ID、吧龄、个性签名
  - 关注 / 粉丝 / 发帖 / 回帖 / 获赞 五项计数
  - 五个快捷入口：**主题帖 / 回帖 / 关注 / 粉丝 / 关注的吧**
  - 未登录状态下同样可用（游客查询）
- **「TA 的回帖」** —— 查看指定用户的回帖记录，可点进原帖定位楼层

### 「我的贴吧」聚合页（本分支新增）

主页左上角**头像**即为入口，一页聚合 4 组共 10 个功能入口：

| 分组 | 入口 |
|---|---|
| 社交 | 我的关注、我的粉丝、我的消息 |
| 我的内容 | 我的主题帖、我的回帖、我的收藏 |
| 贴吧 | 关注的吧、我的吧务 |
| 应用 | 浏览历史、设置 |

页面顶部为个人资料卡（关注 · 粉丝 · 发帖 · 获赞）。

### 签到与任务

- **一键签到** —— 支持一键签到全部已关注贴吧
- **签到双倍经验** —— 一键签到时以「桌面小组件入口」（`from_widget=1`）方式提交，命中官方双倍经验通道

### 净化与去广告

- 系统性移除广告位与推广入口（基座自带能力）
- 移除**发贴 / 回贴风险提示**弹窗（默认关闭，可在设置中开回）

### 其他

- 深浅色主题、动态取色（MD3 Dynamic Color）
- 顶栏着色开关
- 权限申请**直接拉起系统原生弹窗**，不经过二次确认中转
- 设置页内置**感谢名单**

---

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 | Kotlin 2.3.21 |
| UI | Jetpack Compose（BOM 2026.05.01）+ Material Design 3 |
| 导航 | Compose Destinations 1.11.9 + Navigation Compose 2.9.8 |
| 依赖注入 | Hilt 2.58 + KSP 2.3.8 |
| 网络 | OkHttp 5.3.2 + Retrofit 3.0.0 |
| 序列化 | kotlinx.serialization 1.11.0 + **Wire 6.4.0**（Protocol Buffers） |
| 图片 | Glide 5.0.7 + Sketch 3.3.2 |
| 本地存储 | Room 2.7.1 |
| 视频 | AndroidX Media3 1.10.1 |
| 动画 | Lottie 6.7.1 |
| 构建 | AGP 8.13.2 · Gradle 8.14.5 · JDK 17 |

| 项 | 值 |
|---|---|
| 包名 | `com.tieba` |
| minSdk / targetSdk / compileSdk | 23 / 36 / 36 |
| 当前版本 | 1.6.5 (versionCode 23) |

### 关于接口

贴吧的客户端私有接口以 **Protobuf over HTTP** 为主（按 `cmd` 参数路由），本项目通过 Wire 生成的模型直接通信。
部分接口存在服务端缺陷或下线情况，代码中已做降级与兜底处理（例如「TA 的回帖」在官方接口返回空时自动切换到只读镜像数据源）。

---

## 编译

1. **准备环境**：JDK 17、Android SDK（compileSdk 36 / build-tools 36.0.0）
2. **配置 SDK 路径**：在项目根目录创建 `local.properties`
   ```properties
   sdk.dir=/path/to/your/android-sdk
   ```
3. **配置签名**（可选，不配置则使用 debug 签名）：创建 `keystore.properties`
   ```properties
   keystore.file=your.keystore
   keystore.password=******
   keystore.key.alias=******
   keystore.key.password=******
   ```
4. **构建**
   ```bash
   ./gradlew assembleRelease
   ```
   产物位于 `app/build/outputs/apk/release/`。

> 首次构建需联网拉取依赖；若本地已有 Gradle 缓存，可加 `--offline` 加速。

--
## 开源协议

本项目基于 **GNU General Public License v3.0（GPL-3.0）** 发布，完整条款见 [LICENSE](./LICENSE)。

```
Copyright (C) 2026  TiebaApp contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

### 这意味着

- ✅ 你可以自由使用、修改、分发本项目
- ✅ 分发修改后的版本时，**必须同样以 GPL-3.0 开源**并提供完整源码
- ✅ 必须保留原始版权声明与作者署名
- ❌ **不可将本项目用于商业用途**（与原项目一致的要求）

> 本项目继承自 TiebaLite（GPL-3.0），因此**必须**继续以 GPL-3.0 发布 —— 这是许可证的传染性要求，而非可选项。

---

## 致谢

本项目的每一行代码都站在许多人的肩膀上。**特别感谢以下项目与作者**（排名不分先后）：

### 上游项目

| 项目 | 作者 | 说明 |
|---|---|---|
| [**HuanCheng65/TiebaLite**](https://github.com/HuanCheng65/TiebaLite) | [@HuanCheng65](https://github.com/HuanCheng65) | **本项目的直接上游与基石**。贴吧 Lite 的原始实现，整个 Compose 版贴吧客户端的开创性工作 |
| [makise2060/FluxTie](https://github.com/makise2060/FluxTie) | [@makise2060](https://github.com/makise2060) | 本分支所继承的 4.0-dev 演进版本 |

### 接口与协议参考

| 项目 | 作者 | 说明 |
|---|---|---|
| [Starry-OvO/aiotieba](https://github.com/Starry-OvO/aiotieba) | [@Starry-OvO](https://github.com/Starry-OvO) | 异步贴吧客户端库，接口行为的重要参考 |
| [n0099/tbclient.protobuf](https://github.com/n0099/tbclient.protobuf) | [@n0099](https://github.com/n0099) | 百度贴吧 Protobuf 
### 社区

感谢所有实测反馈、提交 Issue 与建议的用户 —— 每一个 bug 报告都在让这个客户端变得更好。

同时感谢贴吧社区中无偿分享接口研究与逆向经验的人们。

---

<div align="center">

**如果这个项目对你有帮助，欢迎点一个 ⭐ Star 支持一下。**

**免责声明：本软件为第三方非官方客户端，仅供学习交流。使用本软件产生的任何后果由使用者自行承担，作者不对任何数据丢失或账号问题负责。**

</div>
