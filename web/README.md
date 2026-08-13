# 乐器调音器 · 网页版

单文件网页版调音器（`index.html`），零依赖，功能与安卓版一致：

- 7 种乐器：吉他 / 贝斯 / 小提琴 / 中提琴 / 大提琴 / 二胡 / 古筝
- YIN 音高检测 + 指针仪表盘 + 智能/手动模式 + 纠错提示（拧反方向/调错弦）+ 自定义调弦
- iPhone/安卓/电脑浏览器都能用（需要麦克风权限）

## 重要前提：必须用 HTTPS 或 localhost 访问

浏览器只允许在「安全上下文」里访问麦克风，所以：

- ❌ 直接双击 `index.html` 用 `file://` 打开 → 麦克风不可用
- ✅ 本地测试：起一个本地服务器，用 `http://localhost` 访问
- ✅ 分享给朋友：部署到任意支持 HTTPS 的静态托管，发链接

## 方式一：本地测试（最快）

```bash
cd GuitarTuner/web
python -m http.server 8000
# 手机和电脑在同一 Wi-Fi 下，手机浏览器访问 http://<电脑IP>:8000 也能测
```

然后用电脑/手机浏览器打开 `http://localhost:8000`（手机则用电脑局域网 IP）。

## 方式二：部署到 GitHub Pages（免费，发链接给朋友）

1. 新建一个 GitHub 仓库（如 `tuner`），把 `index.html` 传上去。
2. 仓库 → Settings → Pages → Source 选 `main` 分支根目录 → Save。
3. 几分钟后得到地址 `https://<你的用户名>.github.io/tuner/`，发给朋友即可。

## 方式三：Netlify Drop（最傻瓜，拖拽即上线）

1. 打开 https://app.netlify.com/drop
2. 把整个 `web` 文件夹拖进去，几秒后得到一个 `https://xxx.netlify.app` 链接。

## 添加到主屏幕（更像 App）

手机浏览器打开页面后：
- **iPhone Safari**：点「分享」→「添加到主屏幕」，桌面会出现图标，点开全屏运行。
- **安卓 Chrome**：菜单 →「添加到主屏幕」。

## 提示

- 首次打开会请求麦克风权限，点「开始调音」并允许即可。
- 古筝 21 根弦的弦选择条可横向滑动。
