# 中药材管理 Android 版

原生 Android 项目，使用 Kotlin + Jetpack Compose，实现 iOS 版中药材管理 App 的本地离线功能。

## 已迁移功能

- 首页统计、快速查询与库存预警
- 药材新增、编辑、删除、撤销本次新增、历史新增记录
- 低库存清单、库存预警值、日用量、价格管理
- 快速入库与支出；按行格式校验、药材存在校验、库存不足提示、二次确认、记录清空
- 小数支出与每味药独立的累计进一余额
- 处方保存、费用计算、医保报销、重要处方保护
- 处方多选、变量对比复制、超过两条处方时 Excel 导出
- 药材库存 Excel 导入导出；低库存整行标红
- JSON 数据库备份与恢复
- 药材资料录入、绿色倾斜“药性”标记、JSON 导出
- 余量密码清零：首次设置密码，之后输入密码二次确认；只清库存，不清其它资料
- APP 说明书

## 打开与安装

1. 使用 Android Studio 打开本仓库根目录。
2. 选择 JDK 17，等待 Gradle 同步完成。
3. 连接安卓手机并开启 USB 调试，或启动模拟器。
4. 点击 Run，或执行：

```bash
gradle :app:assembleDebug
```

生成文件位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

仓库已配置 GitHub Actions：推送到 `main` 后会构建 Debug APK 并上传为 workflow artifact。

## 数据兼容说明

Android 与 iOS 的本地数据库格式不同，iOS 的 Realm 备份不能直接在 Android 中恢复。迁移既有药材库存时，请先从 iOS 导出 Excel，再在 Android 的“数据库管理”中导入；处方和历史记录可按 Android JSON 备份机制独立管理。
