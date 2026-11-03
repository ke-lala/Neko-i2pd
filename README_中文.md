# i2pd Android · 黑白细线版

基于官方 i2pd Android 3.0.0 源码的黑白细线版，保留原有功能并新增 Android VPN 分应用路由，不包含此前 Neko-i2pd 的代码或界面改动。

## 这次改了什么

以 Lokinet 官网的黑白留白、细线和几何线框语言为参考，重新统一明暗配色、导航、状态首页、操作卡片、日志、隧道与设置页。使用原创轻量线框球体和黑白启动图标，不复制 Lokinet 品牌或网站图片；不新增广告、追踪、账号或 UI 动画。

启动、优雅停止、长按立即停止、取消停止、重启、代理服务、Web 控制台、图表、隧道配置、日志搜索／分享、基础／高级／原始配置、数据管理、种子 RPC、后台服务、开机自启和安全屏幕设置均保留。首页默认只显示状态、网络和运行时间，速度、流量、隧道、服务与图表可按需展开。设置页提供明确的“i2pd 全部配置”入口，并新增本地文件补种选择。另新增 VPN 总开关、全局／分应用路由与应用选择；首次开启需确认 Android 系统 VPN 授权。VPN 将流量送入本机 i2pd SOCKS 代理，不会创建通用互联网出口：默认主要用于 I2P 服务，访问普通公网目的地需另行配置 i2pd SOCKS 外出代理；依赖 UDP 的应用可能无法工作。图表仍用实线／虚线／点线与灰度区分数据，界面保留系统、浅色和深色主题。

## 来源与版本

- 官方 Android 仓库：https://github.com/PurpleI2P/i2pd-android
- Android 基线：`df7e2420b6a5108caa7463bde420d3ab48ff2888`（master，2026-10-24）
- 内置 i2pd 源码：`56958857c9f00b1de2cbbe55dd3a853310e608be`，版本 3.0.0；截至 2026-11-02，[上游最新稳定版](https://github.com/PurpleI2P/i2pd/releases/tag/3.0.0)也是 3.0.0。此提交比正式标签多一个 Android 种子 RPC 支持提交，保留该更新。
- VPN 转发：[HevSocks5Tunnel 2.19.3](https://github.com/heiher/hev-socks5-tunnel/releases/tag/2.19.3)（MIT）。上游预编译 AAR 声明最低 API 29；为支持 Android 8，本 ZIP 使用同一固定版本的完整源码，以 NDK API 23 重新构建，代码、生成好的 AAR 和许可证均随包提供。
- 此黑白版为非官方构建，并非官方签名发行。
- 视觉参考：https://lokinet.org/

本 ZIP 包含完整 Android 工程、`binary/jni/i2pd` 实际源码、`third_party/hev-socks5-tunnel` 源码与 `app/libs/hev-socks5-tunnel-2.19.3.aar`，解压后无需重新克隆这些源码或下载 AAR。保留上游 BSD-3-Clause、Hev MIT 许可及其他第三方许可。Gradle／Android SDK／NDK 与 i2pd 原生编译依赖仍需本机安装或下载，不是离线工具链镜像。

## 手机安装

最低 Android 6.0（API 23），Android 8.0/8.1 在版本支持范围内。华为畅享 9 优先使用包含 `armeabi-v7a` 的 APK；通用 APK 同时含多种架构，无需判断系统是 32 位还是 64 位。

本构建的应用包名保持 `org.purplei2p.i2pd`，未更改上游功能相关标识。提供的 APK 使用本地调试签名，不能直接覆盖官方、F-Droid 或其他签名不同的安装。遇到“签名不一致／应用未安装”时，不要先删除原应用；请先在原客户端导出并确认备份可恢复，再决定是否换装。卸载会清除应用私有数据。本工程可自行使用个人密钥构建签名发布版本。

Android 8 安装时，按系统提示允许当前文件管理器／浏览器安装未知来源应用。华为系统如会回收后台进程，请按原客户端的电池优化与自启动设置引导操作；UI 换肤不会绕过系统省电限制。

## 编译环境

Linux（例如 Ubuntu），完整 JDK 17+（必须包含 `javac`），`make`、`cmake`、`perl`、`curl`、`unzip`、`zip`。Gradle Wrapper 已随工程提供。

当前上游工具链版本如下，未为 UI 修改降级：

| 工具 | 版本 |
| --- | --- |
| compileSdk | 37（SDK 包名 `platforms;android-37.0`） |
| targetSdk / minSdk | 36 / 23 |
| Android NDK | 29.0.14206865 |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Gradle Wrapper | 9.7.1 |

### 第一步：准备 SDK

安装 Android 官方命令行工具，并替换下面示例路径：

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/29.0.14206865"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  'platforms;android-37.0' 'build-tools;36.0.0' \
  'ndk;29.0.14206865' 'platform-tools'
```

### 第二步：构建

```bash
cd i2pd-minimal-bw
chmod +x gradlew build-ui.sh binary/jni/*.sh
./build-ui.sh
```

脚本依次校验环境、按上游固定版本／SHA256 下载原生依赖、编译 OpenSSL、编译全部 APK。首次编译会下载 Boost、OpenSSL、miniupnpc、Gradle 和 Maven 依赖，耗时明显长于再次编译。原生依赖结果会缓存在 `binary/jni/deps`。如需从随包 Hev 源码重建 Android 6+ 兼容的 AAR，可先运行 `bash tools/build-hev-aar.sh`（需要已安装的 NDK 29 与完整 JDK）。

也可以按上游命令执行：

```bash
binary/jni/build.sh
./gradlew --max-workers=2 assembleDebug
```

产物在 `app/build/outputs/apk/debug/`，含通用与分架构 APK。调试版可直接安装；发布签名方式见原始 `README.md`。发布签名密钥不要提交到公开仓库。

### 测试

```bash
./gradlew --max-workers=2 :config:test :service:testDebugUnitTest :app:testDebugUnitTest
./gradlew --max-workers=2 lintDebug
```

实际本次构建／测试和运行验证结果见 `验证结果.md`。未进行的测试不会写为通过；模拟器结果不能替代华为实机长时间联网、耗电和后台保活验证。

## 改动位置

- `MinimalTheme.kt`：固定黑白明暗主题、排版和小圆角。
- `LineComponents.kt`：细线卡片与无动画线框图形。
- `ScreenHeading.kt`：紧凑页标题。
- `MainActivity.kt`、`AppScaffold.kt`：接入主题与导航外观，不改原路由及安全逻辑。
- `StatusScreen.kt`：首页布局、按钮层级、状态分隔与图表线型。
- `SettingsScreen.kt`、`ReseedFileScreen.kt`、`LogsScreen.kt`、`TunnelsScreen.kt`：黑白布局；设置页提供全部 i2pd 参数入口、本地补种文件选择，以及 VPN 开关、模式与分应用列表。
- `I2pdVpnService.kt`、`VpnTunnelConfig.kt`：系统 VPN 授权、TUN 接口、应用路由、Hev 映射 DNS 配置和到本地 i2pd SOCKS 的转发。
- `app/src/test/.../VpnTunnelConfigTest.kt`：映射 DNS 地址池及 SOCKS 参数回归测试。
- `AppSettings.kt`：持久化 VPN 开关和按应用选择。
- `CoachMark.kt`、`RouterConfigScreens.kt`、`TorrentsScreen.kt`：只替换卡片视觉外壳。
- `res/values/themes.xml`、`res/values-night/themes.xml`：启动窗口配色。
- `res/drawable/ic_line_launcher.xml`、Manifest 的 icon 引用：黑白启动图标。

原有路由、加密、协议、JNI、daemon、config 和默认配置保持不变；新 VPN 服务只把 Android TUN 流量交给本地 i2pd SOCKS，并不会改写 i2pd 核心协议。

## VPN 下 `.i2p` 域名解析修复

VPN 需要把系统 DNS 查询交给 Hev 的映射 DNS：先返回临时 IPv4 地址，再把该地址还原为原始主机名并通过 i2pd SOCKS5 发送。旧配置使用 `240.0.0.0/4` 作为临时地址池，属于保留地址范围；[Hev 上游配置示例](https://github.com/heiher/hev-socks5-tunnel/blob/main/conf/main.yml)使用 `100.64.0.0/10`。现已改用该上游地址池，并将配置生成抽出为可回归测试的代码，避免 Android／浏览器解析器因特殊保留地址处理而丢失 `.i2p` 目标。此更改修复的是 VPN DNS→SOCKS 主机名转发路径，不会自动添加缺失的 i2p 地址簿记录，也不把 i2pd 变成普通公网出口。

安装修复版后，将应用内 VPN 开关关闭再开启一次，让 Hev 隧道重新加载配置。若系统提示签名不一致，先在旧应用导出并确认备份可恢复，再处理安装；不要为排错直接清除应用数据。

## 3.0.0 本次数据目录故障修复

源码检查确认，`DataDirPreparer` 首次启动会把 `certificates/` 和 `torrents/web/` 从 APK 的 `assets` 复制到私有数据目录，但先前交付的工程没有包含这两组 APK 资源。首次启动在复制证书目录时会失败，并显示“无法准备数据目录”；证书报错是同一缺失证书资源故障的后续表现，不是华为畅享 9 的存储权限问题，也不是用户证书本身有误。修复版已从随 ZIP 提供的固定 i2pd 上游源码补入 25 个证书文件和 3 个 torrent 网页文件；`DataDirPreparer` 也会在后续启动时补回数据目录中缺失的证书，`build-ui.sh` 加入非空检查以防 APK 再次漏包。

请使用修复版 APK 重试。若 Android 提示“签名不一致”或不允许覆盖旧版，先在旧应用内导出数据并确认备份可用，再决定是否卸载；卸载会清除旧应用私有目录。修复版保持包名和版本号不变，未对华为畅享 9 实机运行作未经验证的承诺。

## 首页与文件补种

首页收起非必要指标，运行状态卡默认只显示网络与运行时间；点“显示详情”可查看速度、流量、隧道、NetDB、中转状态、服务快捷入口和历史图表。所有信息仍在应用内，没有删除原统计功能。

“设置 → 路由器 → i2pd 全部配置”列出此 APK 支持且适合在 Android 上编辑的 i2pd 参数；路径、守护进程等平台专用项仍由“编辑 i2pd.conf”文本页处理。文件补种入口接受 `.su3` 或 `.zip`，最多 128 MB，导入后存到应用私有目录并自动设置 `reseed.file` 或 `reseed.zipfile`，同时清除另一种格式的设置。i2pd 在需要网络引导数据时读取该文件；运行中修改配置时按页面提示重启路由器。i2pd 会验证 SU3 签名；ZIP 文件不具备同等签名保证，请只导入可信来源的压缩包。选择“改用网络服务器”可清除手动文件设置。
