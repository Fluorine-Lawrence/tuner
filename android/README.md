# 乐器调音器 · 安卓版（原生 Kotlin）

一个原生安卓 App，两大板块：**调音器** + **练习统计**。底部导航切换，深色专业仪器风 UI。

## 支持的乐器

吉他（15 套调弦）、贝斯、小提琴、中提琴、大提琴、二胡、古筝（21 弦）。

## 功能

### 调音器
- 实时调音：麦克风收音 → YIN 音高检测 → 指针仪表盘（±100 音分），青绿发光调准反馈
- 智能模式（自动识弦）/ 手动模式（点选弦）
- 纠错提示：拧反方向、调错弦
- 各乐器内置调弦预设 + 自定义调弦（自绘选择弹窗，无 Spinner）

### 练习统计
- **完整计时器**：开始/暂停/继续/结束，多段累计
- **曲库**：自由增删曲目，三状态切换（想演奏 / 正在练 / 已拿下），手动选择当前练习曲目
- **练习日志**：每次练完可写心得
- **统计**：按乐器 + 天/周/月/年自绘柱状图，累计/今日/连续天数激励数字
- **每日目标**：自绘滚轮选择器（iOS 风格轮盘）设定，圆角进度条显示达标
- 数据全部本地 SQLite 存储

## 构建方法

1. 用 Android Studio 打开本目录 `android/`，等待 Gradle 同步。
2. 依赖：Material Components（`androidx.appcompat` + `material` + `fragment-ktx`），已配置阿里云镜像。
3. 连接真机（需麦克风），点击 ▶ 运行。

## 目录结构

```
app/src/main/java/com/guitartuner/
├── MainActivity.kt            # 主容器：底部导航（调音器 / 练习）
├── audio/                     # YIN 音高检测 + AudioRecord 采集
├── tuning/                    # 音名/乐器/调弦预设/持久化
├── tuner/                     # 调音引擎 + 纠错逻辑
├── practice/                  # 练习统计
│   ├── db/                    # SQLite（曲目/会话/目标）
│   ├── model/                 # Piece / PracticeSession / PieceStatus
│   ├── TimerController.kt     # 计时核心
│   └── ui/                    # PracticeFragment / Stats / Repertoire / 图表
└── ui/                        # TunerFragment / 仪表盘 / 自绘弹窗 / 滚轮
```

## 技术要点

- 音高检测：YIN 算法（差分 + 累积均值归一化 + 抛物线插值），覆盖 B0(31Hz)~G6(1568Hz)
- 自绘组件（去安卓原生味）：仪表盘、滚轮选择器、弹窗、柱状图、进度条
- 平滑动效：ValueAnimator + DecelerateInterpolator（指针摆动、柱生长、页面淡入）
- 存储：SQLite（练习数据）+ SharedPreferences（调弦/设置）
