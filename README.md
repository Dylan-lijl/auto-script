# Auto-Script (安卓脚本精灵) 🚀

[English](./README_en.md)|简体中文

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE) [![Platform](https://img.shields.io/badge/Platform-Android-green.svg)]() [![JDK](https://img.shields.io/badge/JDK-11-blue.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/11/) [![Language](https://img.shields.io/badge/Language-Java-orange.svg)]() [![Platform](https://img.shields.io/badge/Android-8.0%2B-green.svg?logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)

**Auto-Script** 是一款基于 Java 开发的安卓端自动化脚本执行工具。它可以帮助用户通过自定义规则实现屏幕自动点击、滑动、任务编排等功能，旨在简化重复性操作，提升使用效率。

---

## 项目地址

GitHub:[https://github.com/Dylan-lijl/auto-script](https://github.com/Dylan-lijl/auto-script)<br>
gitee:[https://gitee.com/Dylan-lijl/auto-script](https://gitee.com/Dylan-lijl/auto-script)

>  如果有问题请提交到GitHub的issue

---

## 📺 效果演示

我们提供了一个直观的演示视频来展示脚本的运行过程：

> **查看演示视频**：[点击播放 doc/demo.mp4](doc/demo.mp4)

<img src="doc/record.gif" width="300" />
<img src="doc/replay.gif" width="300" />

---

## ✨ 核心特性

- 🧩 **组件化设计**：采用模块化架构（`app`, `ui-components`, `components-rules`），逻辑清晰，方便二次开发。
- 🎨 **精美 UI**：基于 **QMUI** 框架打造，提供流畅且符合安卓现代设计规范的用户界面。
- 🔄 **版本检查**：内置版本更新检查功能，方便及时获取最新特性。

---

## 📂 项目结构

```text
auto-script/
├── app/                # 主程序模块，包含核心业务逻辑
├── ui-components/      # UI 组件库，基于 QMUI 封装
├── components-rules/   # 组件库xml校验器
├── doc/                # 文档与演示媒体文件 (demo.mp4)
├── gradle/             # Gradle 相关构建配置
└── build.gradle        # 项目全局构建脚本
```

## 🛠️ 编译与安装

1. **克隆仓库**
   
   ```bash
   git clone [https://github.com/Dylan-lijl/auto-script.git](https://github.com/Dylan-lijl/auto-script.git)
   ```
2. **环境要求**
   
   - Android Studio Dolphin 或更高版本
   - JDK 11+
   - Android SDK API 21+
   - Lombok 插件
3. **构建项目**
   在 Android Studio 中打开项目，等待 Gradle 同步完毕，点击工具栏的 `Run` 按钮即可部署到真机或模拟器。

---

## 📝 使用指南

1. **授予权限**：应用运行前必须手动开启“无障碍服务 (Accessibility Service)”以及“悬浮窗”权限，否则脚本无法模拟点击。
2. **编写/导入规则**：在 App 的脚本列表页面点击录制脚本来录制,录制完成后在详情页面，你可以继续添加,编辑,删除等等点击、滑动或长按等动作。
3. **运行脚本**：点击“启动”悬浮按钮，切换至目标 App，脚本将自动根据预设规则执行。

---

## 🤝 参与贡献

---

## 📜 开源协议

本项目采用 [MIT License](LICENSE) 协议。

