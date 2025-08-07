# HopeCraft - Bukkit 高可用功能增强插件
[![GitHub](https://img.shields.io/badge/GitHub-源码-blue?logo=github)](https://github.com/BusyMitten/HopeCraft)  
[![License](https://img.shields.io/badge/License-MPL--2.0-orange)](https://www.mozilla.org/en-US/MPL/2.0/)

专为 **Bukkit 1.21+** 设计的轻量级工具集，提供声明式菜单系统与核心增强功能，性能开销逼近零。

## 🚀 核心价值
- **声明式菜单引擎** - 通过YAML配置自定义GUI菜单，无需重载
- **超轻量内核** - 基于Bukkit事件驱动模型，常驻内存 < 5MB

## ⚙️ 硬性要求
| 组件            | 最低版本       | 推荐链接                     |  
|----------------|--------------|----------------------------|  
| **Java**       | JDK 17       | [Adoptium](https://adoptium.net/) |  
| **服务端核心**   | Bukkit 1.21+ | [PaperMC](https://papermc.io/) |  
| **构建工具**     | Maven 3.9+   | [Maven](https://maven.apache.org/) |  

## 🛠️ 构建指南（Linux/macOS）
```
bash

git clone https://github.com/BusyMitten/HopeCraft.git

cd HopeCraft

mvn clean package -DskipTests # 产出位于 target/
```
> **开发建议**：使用 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 导入 Maven 项目

## 📦 安装流程
1. 将 `target/HopeCraft-*.jar` 置于服务端 `plugins/`
2. **重启服务端**（首次加载必需）
3. 按需编辑生成的 `plugins/HopeCraft/config.yml`

## ⚖️ 开源协议
**MPL-2.0** 强制约束：
1. 所有衍生代码必须开源
2. 修改文件头部保留原始版权声明
3. 闭源产品需分离插件代码与自有代码
> [完整条款](https://www.mozilla.org/en-US/MPL/2.0/) | [常见问题](https://www.mozilla.org/en-US/MPL/2.0/FAQ/)

## 🧩 项目结构
```
access transformers
HopeCraft/

├── src/main/ # Java 业务逻辑

├── pom.xml # Maven 依赖及构建设置

└── target/ # 编译产出目录 (构建后生成)
```
> 注：IDE相关文件（.iml）已通过.gitignore过滤

## 🤝 贡献之道
**我们急需以下帮助**：
- ✅ 文档国际化（英文README优先）


## 🚨 技术支持
| 问题类型               | 响应时效     | 沟通渠道                                     |  
|------------------------|------------|--------------------------------------------|  
| **致命错误** (Crash)   | < 24小时    | [GitHub Issues](https://github.com/BusyMitten/HopeCraft/issues) |  
| **功能请求**           | < 3天       | [Discussions](https://github.com/BusyMitten/HopeCraft/discussions) |  
| **配置疑难**           | 社区互助     | [Wiki文档](https://github.com/BusyMitten/HopeCraft/wiki) |  

---  
**核心维护**: [BusyMitten](https://github.com/BusyMitten) | [NanoTurtle1145](https://github.com/NanoTurtle1145)  
**最后更新**: 2025年8月8日