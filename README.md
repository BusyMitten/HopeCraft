# HopeCraft - Bukkit核心功能增强插件
[![GitHub](https://img.shields.io/badge/GitHub-源码-blue?logo=github)](https://github.com/BusyMitten/HopeCraft)  
[![License](https://img.shields.io/badge/License-MPL--2.0-orange)](https://www.mozilla.org/en-US/MPL/2.0/)

专为 **Bukkit 1.21+** 设计的轻量级工具集。

## 🚀 核心功能
实现菜单功能，代码简洁易读写，拓展空间大。

## ⚙️ 硬性要求
| 组件            | 最低版本       | 推荐链接                     |  
|----------------|--------------|----------------------------|  
| **Java**       | JDK 17       | [Adoptium](https://adoptium.net/) |  
| **服务端核心**   | Bukkit 1.21+ | [PaperMC](https://papermc.io/) |  
| **构建工具**     | Maven 3.9+   | [Maven](https://maven.apache.org/) |  

## 🛠️ 如何构建（Linux/macOS（Windows建议使用Git Bash））
```
bash

git clone https://github.com/BusyMitten/HopeCraft.git

cd HopeCraft

mvn clean package -DskipTests # 产出位于 target/
```

## 📦 安装流程
1. 将 `target/HopeCraft-*.jar` 置于服务端 `plugins/`
2. **重启服务端**（首次加载必需）
3. 按需编辑生成的 `plugins/HopeCraft/config.yml`


## 🧩 项目结构
```
access transformers
HopeCraft/

├── src/main/ # Java 业务逻辑

├── pom.xml # Maven 依赖及构建设置

└── target/ # 编译产出目录 (构建后生成)
```
> 注：IDE相关文件（.iml）已通过.gitignore过滤
            

---  
**核心维护**: [BusyMitten](https://github.com/BusyMitten) | [NanoTurtle1145](https://github.com/NanoTurtle1145)  
**最后更新**: 2025年8月8日
> 暑假时间多，好好珍惜来之不易的开发时光吧
