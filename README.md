# 乐器调音器

支持 7 种乐器（吉他 / 贝斯 / 小提琴 / 中提琴 / 大提琴 / 二胡 / 古筝）的开源调音器，提供**网页版**和**安卓版**两个版本，功能一致：

- YIN 音高检测 + 指针仪表盘（±100 音分）
- 智能模式（自动识弦） / 手动模式（点选弦）
- 纠错提示：拧反方向、调错弦
- 各乐器内置调弦预设 + 自定义调弦

## 目录

```
├── index.html       # 网页版（单文件，Gitee/GitHub Pages 部署用）
├── web/README.md    # 网页版部署说明
├── android/         # 安卓版（原生 Kotlin，含编译好的 APK）
└── LICENSE          # MIT 开源许可证
```

## 快速使用

- **网页版**：用手机浏览器打开部署好的链接（Gitee Pages 或 GitHub Pages），点「开始调音」授权麦克风即可。详见 [`web/README.md`](web/README.md)。
- **安卓版**：下载 [`android/release/GuitarTuner-v1.4.apk`](android/release/GuitarTuner-v1.4.apk) 安装（Android 7.0+）。

## 许可证

[MIT](LICENSE)
