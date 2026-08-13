# 乐器调音器 · 安卓版（原生 Kotlin）

一个**原生安卓**调音器 App，支持 7 种乐器，纯 Kotlin + Android framework 实现，**零第三方依赖**，可离线编译。

## 支持的乐器

吉他（15 套调弦）、贝斯、小提琴、中提琴、大提琴、二胡、古筝（21 弦）。

## 功能

- **实时调音**：麦克风收音 → YIN 音高检测 → 指针仪表盘（±100 音分），红→黄→绿分区，居中变绿即调准。
- **两种模式**（底部按钮切换）：
  - **智能模式**：自动识别你正在拨哪根弦并高亮，拨弦即得结果。
  - **手动模式**：点选某根弦，只针对该弦显示偏差。
- **纠错提示**（顶部胶囊标签）：
  - 拧**反方向**：音高越拧越偏离目标 → 提示「⚠ 拧反方向了，反着拧」。
  - 调**错弦**：手动模式下，拨的弦更像另一根 → 提示「⚠ 可能调错弦」。
- **自定义调弦**：自定名称、逐弦选音名+八度，持久化保存，可长按删除。
- **频率平滑**：对检测频率做指数平滑，抑制拨弦瞬间的冲高/抖动。

## 直接安装

仓库 `android/release/GuitarTuner-v1.4.apk` 是编译好的安装包（Android 7.0+，仅需麦克风权限），下载到手机安装即可。

## 构建方法

1. 用 **Android Studio**（Hedgehog 及以上，自带 JDK 17）打开本目录 `android/`。
2. 等待 Gradle 同步（自动下载 Gradle 8.5 与 AGP 8.2.2 / Kotlin 1.9.22）。
3. 连接真机（需麦克风，模拟器音频受限），点击 ▶ 运行。

> 项目已内置国内镜像仓库（阿里云）与 `android.overridePathCheck=true`，即使本地用户目录含中文也能正常构建。

## 目录结构

```
app/src/main/java/com/guitartuner/
├── MainActivity.kt            # 主界面：乐器切换 + 调音 + 模式切换 + 纠错提示
├── audio/
│   ├── AudioEngine.kt         # AudioRecord 采集（后台线程 + RMS 静音过滤）
│   └── PitchDetector.kt       # YIN 音高检测
├── tuning/
│   ├── Note.kt                # 音名/八度/MIDI/频率换算
│   ├── GuitarString.kt        # 弦模型
│   ├── Tuning.kt              # 调弦 data class
│   ├── Instrument.kt          # 乐器枚举（吉他/贝斯/小提琴/中提琴/大提琴/二胡/古筝）
│   ├── TuningLibrary.kt       # 各乐器内置预设库
│   └── TuningStore.kt         # 自定义调弦 + 当前调弦持久化
├── tuner/
│   ├── TunerEngine.kt         # 频率→音符→弦→偏差→纠错提示
│   └── DirectionMonitor.kt    # 偏差趋势分析（拧反方向检测）
└── ui/
    ├── TunerGaugeView.kt      # 自定义 Canvas 指针仪表盘
    ├── TuningListActivity.kt  # 调弦选择列表
    └── CustomTuningActivity.kt# 自定义调弦编辑器
```

## 技术要点

- **音高检测**：YIN 算法（差分函数 + 累积均值归一化 + 抛物线插值），覆盖贝斯 B0(31Hz)~古筝 G6(1568Hz)。
- **偏差**：`cents = 1200·log2(f / f_target)`；|偏差| ≤ 5 音分判定「已调准」。
- **拧反方向**：维护近 1.2s 偏差历史做线性回归，|偏差| 斜率 > 12 音分/秒即判为越拧越偏。
- **错弦**：手动模式下，检测音高到某根弦的距离比到目标弦小 30 音分以上 → 提示调错弦。
