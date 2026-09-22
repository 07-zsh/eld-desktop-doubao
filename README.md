# 老年桌面（Elder Desktop）

面向完全没接触过智能手机的长辈的大字 Launcher：开机直达、单屏无层级、点一下完成。
依据 `docs/产品设计文档.md` 与 `docs/技术方案.md`（MVP v0.1）实现。

## 已实现范围

- **双形态单 APK**：`LauncherActivity`（HOME/DEFAULT，设为默认桌面后开机直达）+ `MainActivity`（桌面图标，子女设置入口）。
- **桌面首页**：大时钟（低频刷新、秒点动画）+ 日期 + 预置天气，四宫格大按钮（电话/家人/照片/SOS），仅一屏无滑动。
- **电话**：大「拨号」按钮 + 家人通讯录，点头像即 `ACTION_CALL`，失败回退 `ACTION_DIAL`；无实体拨号盘。
- **家人**：同一份通讯录，点头像即拨（按 D3，MVP 不展示视频入口）。
- **照片**：全屏大图 + 左右大箭头 + 圆点指示器；空相册大字提示不崩溃。
- **SOS**：纯 Kotlin 状态机（`Idle → Confirm1 → Countdown(5s) → Triggered`），二次确认 + 倒计时可取消 + 30s 冷却防连点；触发后拨打紧急号码并向家人逐条发送预设短信，失败有兜底。
- **子女设置模式**：唯一允许出现系统权限弹窗的阶段，一次性授予 `CALL_PHONE`/`SEND_SMS` 并从 `assets/preset/*` 预置联系人、紧急号码、相册；完成后可进入「管理家人」增删改。
- **联系人管理（功能1）**：子女可新增/编辑/删除家人（姓名、手机号、头像、紧急标志）；头像经系统文件选择器免权限导入；新增排末尾；紧急标志最多一人（SOS 拨打对象可控）；删除联系人保留头像文件（与相册共用目录）。
- **数据层**：Room（联系人/紧急号码，支持子女增删改）+ DataStore（已初始化/SOS 冷却）+ 应用私有目录（相册）；无服务器、无账号、无后台服务、无定位权限。
- **架构**：MVVM + 单向数据流；手工 DI 容器 `AppContainer`（技术方案 §2.2 备选，MVP 精简，未引入 Hilt）。

## 构建要求

- JDK 17
- Android SDK：compileSdk/targetSdk 34，minSdk 24（Android 7.0+）
- Gradle 8.7

首次在装有 Android SDK 的机器上：

```bash
cd ElderDesktop
gradle wrapper                 # 生成 gradlew（或直接用 Android Studio 打开）
./gradlew :app:assembleDebug        # 构建 Debug APK
./gradlew :app:testDebugUnitTest   # 运行纯 JVM 单测
```

> 说明：本机已配置 JDK 17 + Android SDK + Gradle 8.7，单测已通过（25 个用例全绿，含功能1 新增 11 个）；`assembleDebug` 生成 APK 可在此环境执行。

## 首次使用

1. 安装 APK，点桌面图标「老年桌面」进入子女设置。
2. 授予电话/短信权限，自动导入 `assets/preset/` 预置数据。
3. （可选）在设置页点「管理家人」增删改联系人（姓名/手机号/头像/紧急标志）。
4. 按提示在系统设置中把「老年桌面」设为默认桌面。
5. 之后长辈开机即入大字桌面，全程零弹窗、零设置。

## 预置数据位置

子女可直接编辑后随 APK 发布：

- `ElderDesktop/app/src/main/assets/preset/contacts.json`
- `ElderDesktop/app/src/main/assets/preset/emergency.json`
- `ElderDesktop/app/src/main/assets/preset/weather.json`
- `ElderDesktop/app/src/main/assets/preset/photos/`（相册图片）

## 待产品确认（技术方案 §13）

1. D1：子女侧首启授权 + 数据预置流程是否可接受。
2. D2：MVP 天气使用子女预置/占位数据（不联网、不定位）。
3. D3：家人模块视频入口「不展示」（当前实现）还是「置灰禁用」。
4. minSdk 取 24 是否需要按实测老年机分布调整。
