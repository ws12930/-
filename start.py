#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""7h 博客一键启动脚本

双击 start-all.bat 即可，本脚本会依次完成：
  1. 预检：前端构建产物是否完整、后端 mvnw 是否存在
  2. 数据库自检：库不存在时自动建库并导入 springBootTest/fenglin_blog.sql
  3. 拉起 Spring Boot 后端（8081），若已在运行则复用
  4. 启动前端联调服务（4173 起）：托管 7h/dist 静态资源 + 反代 /api、/uploads 到后端
  5. 等两个服务都就绪后自动打开浏览器

与旧版的差别（都是踩过的坑）：
  * 上传目录不再写死 C:\\Users\\user，改为当前用户目录；
  * 后端不再开独立窗口，输出落到 springBootTest/log/startup.log，失败时把末尾日志直接打出来；
  * 后端没起来就**不打开浏览器**，避免看到一个白屏还以为在跑；
  * 4173 被别的程序占用时不再假装“前端已在运行”，会自动顺延到 4174、4175……
  * 后端连不上数据库（库不存在）时自动建库导入 SQL，不用手动开 Navicat。

按 Ctrl+C 退出，会连带关闭后端进程。

对外地址默认是局域网 IP：http://192.168.68.77:4173/
  * 前端服务监听 0.0.0.0:4173，同网段的手机 / 其它电脑可直接访问；
  * 后端监听 0.0.0.0:8081，前端通过反向代理调用，浏览器侧无需关心端口；
  * 换网段时用环境变量 BLOG_HOST 指定新 IP，不必改代码。
"""
import glob
import os
import socket
import subprocess
import sys
import threading
import time
import webbrowser
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DIST_DIR = os.path.join(BASE_DIR, "7h", "dist")
FRONT_DIR = os.path.join(BASE_DIR, "7h")
BACKEND_DIR = os.path.join(BASE_DIR, "springBootTest")
DB_SQL = os.path.join(BACKEND_DIR, "fenglin_blog.sql")
BACKEND_LOG = os.path.join(BACKEND_DIR, "log", "startup.log")

# ---------------------------------------------------------------- 访问地址
# 对外用本机局域网 IP，手机 / 同网段电脑可以直接打开 http://192.168.68.77:4173/。
# 换网段或换机器时用环境变量 BLOG_HOST 覆盖，不用改代码：
#     set BLOG_HOST=192.168.68.100 && start-all.bat
# 想只在本机用回环地址，设 BLOG_HOST=127.0.0.1。
LAN_HOST = os.environ.get("BLOG_HOST", "192.168.68.77")
# 服务实际监听 0.0.0.0（所有网卡）：这样 127.0.0.1 和局域网 IP 都能访问，
# 不会出现“绑定到局域网 IP 后本机访问 localhost 反而不通”的坑。
BIND_HOST = "0.0.0.0"
BACKEND_PORT = 8081
BACKEND = "http://%s:%d" % (LAN_HOST, BACKEND_PORT)
FRONTEND_PORT = 4173
FRONTEND_PORT_TRIES = 12
PROXY_PREFIXES = ("/api/", "/uploads/")
# 后端等待策略：不再是「死等 N 秒就判失败」。
# 首次启动 mvnw 要从中央仓库拉一整棵依赖树，慢的时候能拖十几分钟，
# 但日志一直在增长就说明它没死 —— 只要还在动就把截止时间往后推，
# 只有日志连续 LOG_STALL_SECONDS 没有任何变化，才算真的卡住。
def _env_int(name, default):
    try:
        value = int((os.environ.get(name) or "").strip() or default)
        return value if value > 0 else default
    except ValueError:
        return default


BACKEND_WAIT_SECONDS = _env_int("BLOG_BACKEND_WAIT", 300)
BACKEND_WAIT_MAX = _env_int("BLOG_BACKEND_WAIT_MAX", 1800)
LOG_STALL_SECONDS = 150

DB_NAME = "fenglin_blog"
DB_HOST = os.environ.get("BLOG_DB_HOST", "127.0.0.1")
DB_PORT = os.environ.get("BLOG_DB_PORT", "3306")
DB_USER = os.environ.get("BLOG_DB_USERNAME", "root")
DB_PASSWORD = os.environ.get("BLOG_DB_PASSWORD", "123456")

# 上传文件的持久化目录：放在项目仓库之外。
# 原因：uploads 若留在仓库工作区内，一旦重新 clone / git clean / 换目录，
# 未提交的图片会全部消失（头像、私信图片、文章配图全都遭殃）。
# 注意：必须跟着当前 Windows 用户走，写死 C:\Users\user 在别的机器上会失败。
UPLOAD_DATA_DIR = os.environ.get("BLOG_UPLOAD_DIR") or os.path.join(
    os.path.expanduser("~"), "blog-data", "uploads")
REPO_UPLOAD_DIR = os.path.join(BACKEND_DIR, "uploads")

LAUNCHER_HEADER = "X-7H-Launcher"


# ------------------------------------------------------------------ 小工具
def log(msg):
    print("[%s] %s" % (time.strftime("%H:%M:%S"), msg), flush=True)


def warn(msg):
    print("[%s] [!] %s" % (time.strftime("%H:%M:%S"), msg), flush=True)


def die(msg, hint=None):
    """打印错误 + 可选建议，然后暂停，避免双击运行时窗口一闪而过"""
    print("\n[错误] %s" % msg, flush=True)
    if hint:
        print("       %s" % hint, flush=True)
    if sys.stdin and sys.stdin.isatty():
        try:
            input("\n按回车退出...")
        except (EOFError, KeyboardInterrupt):
            pass
    return 1


def port_open(port, host="127.0.0.1", timeout=0.6):
    s = socket.socket()
    s.settimeout(timeout)
    try:
        return s.connect_ex((host, port)) == 0
    finally:
        s.close()


def host_bindable(host):
    """host 是否是本机某块网卡上的地址（能 bind 就说明是本机 IP）"""
    s = socket.socket()
    try:
        s.bind((host, 0))
        return True
    except OSError:
        return False
    finally:
        s.close()


def resolve_host():
    """确认对外 IP 真的挂在本机上；IP 变了就回退回环地址，避免脚本空等超时"""
    global LAN_HOST, BACKEND
    if LAN_HOST in ("", "0.0.0.0"):
        LAN_HOST = "127.0.0.1"
    elif LAN_HOST not in ("127.0.0.1", "localhost") and not host_bindable(LAN_HOST):
        warn("%s 不是本机网卡上的地址（网段变了？），本次回退到 127.0.0.1" % LAN_HOST)
        warn("如需固定，用环境变量 BLOG_HOST 指定当前 IP")
        LAN_HOST = "127.0.0.1"
    BACKEND = "http://%s:%d" % (LAN_HOST, BACKEND_PORT)


def free_port(start, tries):
    for port in range(start, start + tries):
        if not port_open(port):
            return port
    return None


def tail_file(path, lines=40):
    try:
        with open(path, "rb") as f:
            data = f.read()
    except OSError:
        return ""
    text = data.decode("utf-8", "replace")
    return "\n".join(text.splitlines()[-lines:])


def file_size(path):
    """日志体积，用来判断后端进程还在不在干活"""
    try:
        return os.path.getsize(path)
    except OSError:
        return 0


def last_log_line(path, width=100):
    """日志里最后一行有内容的，播报进度时给用户一个「它还在动」的证据"""
    for line in reversed(tail_file(path, 8).splitlines()):
        s = line.strip()
        if s:
            return s if len(s) <= width else s[:width - 1] + "…"
    return ""


def looks_downloading(path):
    """最近几行是不是还在拉 Maven 依赖 —— 决定超时后该给什么建议"""
    return "Downloading from" in tail_file(path, 12)


# ---------------------------------------------------------------- 数据库自检
def find_mysql():
    """按 PATH / 常见安装位置找 mysql.exe"""
    for name in ("mysql.exe", "mysql"):
        for path in os.environ.get("PATH", "").split(os.pathsep):
            cand = os.path.join(path, name)
            if os.path.isfile(cand):
                return cand
    patterns = [
        r"C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe",
        r"C:\Program Files (x86)\MySQL\MySQL Server *\bin\mysql.exe",
        r"C:\ProgramData\MySQL\MySQL Server *\bin\mysql.exe",
        r"C:\xampp\mysql\bin\mysql.exe",
        r"C:\phpstudy_pro\Extensions\MySQL*\bin\mysql.exe",
        r"D:\phpstudy_pro\Extensions\MySQL*\bin\mysql.exe",
        r"D:\Program Files\MySQL\MySQL Server *\bin\mysql.exe",
    ]
    found = []
    for pattern in patterns:
        found.extend(glob.glob(pattern))
    # 版本号大的优先（8.0 > 5.7）
    if found:
        found.sort(reverse=True)
        return found[0]
    return None


def mysql_cmd(mysql, sql):
    return [mysql,
            "-h", DB_HOST, "-P", str(DB_PORT), "-u", DB_USER,
            "--password=" + DB_PASSWORD,
            "--default-character-set=utf8mb4",
            "-N", "-B", "-e", sql]


def mysql_call(mysql, sql, stdin_path=None):
    stdin = open(stdin_path, "rb") if stdin_path else subprocess.DEVNULL
    try:
        return subprocess.run(mysql_cmd(mysql, sql), stdin=stdin,
                              capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=180)
    finally:
        if stdin_path:
            stdin.close()


def ensure_database():
    """库不存在时自动建库 + 导入初始化脚本。返回 True 表示可以直接往后走"""
    mysql = find_mysql()
    if not mysql:
        warn("没找到 mysql.exe，跳过数据库自检。"
             "如果后端报 Unknown database，请手动导入 %s" % DB_SQL)
        return True
    if not port_open(int(DB_PORT), DB_HOST, 1.5):
        warn("MySQL 没有在 %s:%s 监听，请先启动 MySQL 服务" % (DB_HOST, DB_PORT))
        return True

    probe = mysql_call(mysql, "SELECT 1")
    if probe.returncode != 0:
        detail = (probe.stderr or "").strip().splitlines()
        warn("连不上 MySQL（%s）：%s" % (DB_USER, detail[-1] if detail else "未知错误"))
        warn("请确认 MySQL 的 root 密码，可用环境变量 BLOG_DB_PASSWORD 覆盖（当前 = %s）"
             % ("*" * len(DB_PASSWORD)))
        return True

    exists = mysql_call(mysql, "SHOW DATABASES LIKE '%s'" % DB_NAME)
    if (exists.stdout or "").strip():
        return True

    log("数据库 %s 不存在，正在建库并导入初始化脚本..." % DB_NAME)
    created = mysql_call(mysql, "CREATE DATABASE IF NOT EXISTS `%s` "
                                "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" % DB_NAME)
    if created.returncode != 0:
        warn("建库失败：%s" % (created.stderr or "").strip())
        return True
    if not os.path.isfile(DB_SQL):
        warn("找不到初始化脚本 %s，库是空的，页面会没有数据" % DB_SQL)
        return True
    # 用 stdin 直接灌 SQL（-e 一次只能一条语句，多表建表和注释都会挂）
    imported = subprocess.run(
        [mysql, "-h", DB_HOST, "-P", str(DB_PORT), "-u", DB_USER,
         "--password=" + DB_PASSWORD, "--default-character-set=utf8mb4", DB_NAME],
        stdin=open(DB_SQL, "rb"), capture_output=True, text=True,
        encoding="utf-8", errors="replace", timeout=300)
    if imported.returncode != 0:
        warn("导入 SQL 失败：%s" % (imported.stderr or "").strip()[-300:])
        return True
    log("数据库初始化完成（%s）" % DB_NAME)
    return True


# ---------------------------------------------------------------- 上传目录迁移
def migrate_uploads():
    """把仓库内 uploads 目录里的旧文件搬运到持久化目录（只补缺失，不覆盖、不删除）。"""
    if not os.path.isdir(REPO_UPLOAD_DIR):
        return
    moved = 0
    for root, _dirs, files in os.walk(REPO_UPLOAD_DIR):
        for name in files:
            src = os.path.join(root, name)
            rel = os.path.relpath(src, REPO_UPLOAD_DIR)
            dst = os.path.join(UPLOAD_DATA_DIR, rel)
            if os.path.isfile(dst):
                continue
            try:
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with open(src, "rb") as fin, open(dst, "wb") as fout:
                    fout.write(fin.read())
                moved += 1
            except OSError as e:
                warn("归档上传文件失败：%s（%s）" % (src, e))
    if moved:
        log("已把 %d 个仓库内上传文件归档到 %s" % (moved, UPLOAD_DATA_DIR))


# ---------------------------------------------------------------- 前后端探活
def backend_alive():
    if not port_open(BACKEND_PORT):
        return False
    try:
        with urlopen(BACKEND + "/api/auth/ping", timeout=3) as resp:
            return resp.status == 200
    except HTTPError:
        # 接口存在但不是 200，同样说明端口上是后端
        return True
    except Exception:
        return False


def frontend_alive(port):
    """端口上跑的是不是本脚本的前端服务（靠自定义响应头辨认，避免误认别人的服务）"""
    try:
        with urlopen("http://127.0.0.1:%d/" % port, timeout=3) as resp:
            return resp.headers.get(LAUNCHER_HEADER) == "1"
    except Exception:
        return False


def dist_assets_missing():
    """dist/index.html 引用的 js/css 是否真的存在——缺了就是白屏"""
    index = os.path.join(DIST_DIR, "index.html")
    if not os.path.isfile(index):
        return ["index.html"]
    try:
        html = open(index, encoding="utf-8", errors="replace").read()
    except OSError:
        return ["index.html"]
    import re
    missing = []
    for ref in re.findall(r'(?:src|href)="\.?/?([^"]+\.(?:js|css))"', html):
        if not os.path.isfile(os.path.join(DIST_DIR, ref.replace("/", os.sep))):
            missing.append(ref)
    return missing


# ---------------------------------------------------------------- 前端联调服务
class ProxyHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIST_DIR, **kwargs)

    def _is_proxy(self):
        return any(self.path.startswith(p) for p in PROXY_PREFIXES)

    def _proxy(self, method):
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else None
        headers = {}
        for key in ("Content-Type", "Accept", "Authorization", "X-Requested-With"):
            if self.headers.get(key):
                headers[key] = self.headers.get(key)
        req = Request(BACKEND + self.path, data=body, headers=headers, method=method)
        try:
            with urlopen(req, timeout=60) as resp:
                payload = resp.read()
                self.send_response(resp.status)
                if resp.headers.get("Content-Type"):
                    self.send_header("Content-Type", resp.headers.get("Content-Type"))
                self.send_header("Content-Length", str(len(payload)))
                self.send_header("Cache-Control", "no-store")
                self.end_headers()
                if method != "HEAD":
                    self.wfile.write(payload)
        except HTTPError as e:
            payload = e.read()
            self.send_response(e.code)
            self.send_header("Content-Type", e.headers.get("Content-Type", "application/json;charset=UTF-8"))
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            if method != "HEAD":
                self.wfile.write(payload)
        except URLError as e:
            msg = ("后端未响应：%s" % e.reason).encode("utf-8")
            self.send_response(502)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(msg)))
            self.end_headers()
            if method != "HEAD":
                self.wfile.write(msg)

    def do_GET(self):
        if self._is_proxy():
            self._proxy("GET")
            return
        # SPA 回退：dist 里没有对应静态文件时交给前端路由处理，
        # 否则直接访问 /posts/67 或刷新子页面都会 404。
        if not self._static_exists():
            self.path = "/index.html"
        super().do_GET()

    def do_HEAD(self):
        if self._is_proxy():
            self._proxy("HEAD")
            return
        if not self._static_exists():
            self.path = "/index.html"
        super().do_HEAD()

    def _static_exists(self):
        """当前路径在 dist 里是否真有对应文件；同时挡掉 ../ 之类的路径穿越"""
        rel = self.path.split("?", 1)[0].split("#", 1)[0].lstrip("/")
        root = os.path.abspath(DIST_DIR)
        full = os.path.abspath(os.path.join(root, rel)) if rel else root
        if not full.startswith(root):
            return False
        if os.path.isdir(full):
            full = os.path.join(full, "index.html")
        return os.path.isfile(full)

    def do_POST(self):
        self._proxy("POST") if self._is_proxy() else self.send_error(404)

    def do_PUT(self):
        self._proxy("PUT") if self._is_proxy() else self.send_error(404)

    def do_DELETE(self):
        self._proxy("DELETE") if self._is_proxy() else self.send_error(404)

    def end_headers(self):
        self.send_header(LAUNCHER_HEADER, "1")
        if not self._is_proxy():
            self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, fmt, *args):
        if self._is_proxy():
            sys.stderr.write("  %s - %s\n" % (self.address_string(), fmt % args))


# ------------------------------------------------------------------ 主流程
def start_backend():
    """启动 Spring Boot，返回 Popen；已在运行则返回 None；失败返回 False"""
    if backend_alive():
        log("后端已在运行，直接复用 (%s)" % BACKEND)
        return None

    if port_open(BACKEND_PORT):
        return die("端口 %d 被占用，但不是本项目的后端。" % BACKEND_PORT,
                   "先执行 stop-all.bat，或用 netstat -ano | findstr :%d 找到占用进程" % BACKEND_PORT)

    mvnw = os.path.join(BACKEND_DIR, "mvnw.cmd")
    if not os.path.isfile(mvnw):
        return die("找不到 Maven Wrapper：%s" % mvnw,
                   "确认 springBootTest 目录完整，或改用本机 mvn 启动")

    log("启动后端 Spring Boot (%s)，首次启动需要下载依赖，可能要几分钟..." % BACKEND)
    os.makedirs(UPLOAD_DATA_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(BACKEND_LOG), exist_ok=True)
    migrate_uploads()

    env = dict(os.environ)
    env["BLOG_DB_PASSWORD"] = DB_PASSWORD
    env["BLOG_UPLOAD_DIR"] = UPLOAD_DATA_DIR
    env.pop("SERVER__PORT", None)  # 避免被宽松绑定抢走 server.port

    # 输出直接落文件：旧版开独立窗口，窗口里报错但用户看不到，只能看到“进程意外退出”
    log_file = open(BACKEND_LOG, "wb")
    creation = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    proc = subprocess.Popen(
        # 显式绑 0.0.0.0：默认就是全网卡，但写出来可以避免被 application.properties
        # 里的 server.address 影响，也方便局域网其它设备直连 8081 调试
        ["cmd", "/c", mvnw, "-B", "spring-boot:run",
         "-Dspring-boot.run.arguments=--server.port=%d --server.address=%s"
         % (BACKEND_PORT, BIND_HOST)],
        cwd=BACKEND_DIR,
        env=env,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        creationflags=creation,
    )

    started = time.time()
    deadline = started + BACKEND_WAIT_SECONDS
    hard_deadline = started + BACKEND_WAIT_MAX
    next_notice = started + 15
    last_size = file_size(BACKEND_LOG)
    last_change = started

    while True:
        if backend_alive():
            log("后端已就绪：%s/api（用时 %d 秒）" % (BACKEND, time.time() - started))
            return proc
        if proc.poll() is not None:
            print()
            warn("后端进程已退出（退出码 %s），日志尾部：" % proc.returncode)
            print("-" * 60)
            print(tail_file(BACKEND_LOG, 30))
            print("-" * 60)
            return die("后端启动失败", "完整日志：%s" % BACKEND_LOG)

        now = time.time()
        size = file_size(BACKEND_LOG)
        if size != last_size:
            last_size = size
            last_change = now
            # 日志还在长 = Maven 还在下依赖 / Spring 还在编译，把截止时间往后顺延。
            # 这一条就是本次「超时后 1 秒就启动成功」的根因补丁：
            # 原来只按死时间判失败，等于把正在下载的进程当成卡死。
            if deadline < hard_deadline:
                deadline = min(now + BACKEND_WAIT_SECONDS, hard_deadline)

        if now >= deadline:
            break

        if now >= next_notice:
            log("后端仍在启动中（已等 %d 秒）— %s"
                % (now - started, last_log_line(BACKEND_LOG) or "准备中"))
            next_notice = now + 15
        time.sleep(1)

    stalled_for = int(time.time() - last_change)
    stalled = stalled_for >= LOG_STALL_SECONDS
    warn("等待后端超时（已等 %d 秒，%s），日志尾部："
         % (time.time() - started,
            "日志已 %d 秒没有变化，像是卡住了" % stalled_for if stalled else "一直在下载依赖"))
    print("-" * 60)
    print(tail_file(BACKEND_LOG, 25))
    print("-" * 60)
    # 超时后不要留个孤儿进程占着 8081，否则下次启动会被误判成「端口被占用」
    if proc.poll() is None:
        subprocess.run(["taskkill", "/PID", str(proc.pid), "/T", "/F"], capture_output=True)
        warn("已结束这次的后端进程，避免它继续占着 %d 端口" % BACKEND_PORT)

    hint = "完整日志：%s" % BACKEND_LOG
    if looks_downloading(BACKEND_LOG):
        hint += ("\n       依赖还没下完。可以先把依赖下全，之后启动就是秒开："
                 "\n       cd springBootTest && mvnw -B dependency:go-offline")
    if not stalled:
        hint += ("\n       网络慢就把上限调大：set BLOG_BACKEND_WAIT_MAX=3600 后再运行")
    return die("后端启动超时", hint)


def start_frontend():
    """返回 (server, port)；失败返回 (None, None)"""
    # 同端口上已经跑着本脚本的前端服务，直接复用
    if port_open(FRONTEND_PORT) and frontend_alive(FRONTEND_PORT):
        log("前端服务已在运行，直接复用 (http://%s:%d/)" % (LAN_HOST, FRONTEND_PORT))
        return None, FRONTEND_PORT

    port = FRONTEND_PORT
    if port_open(port):
        warn("端口 %d 被别的程序占用，换一个端口启动前端服务" % port)
        port = free_port(FRONTEND_PORT + 1, FRONTEND_PORT_TRIES)
        if port is None:
            die("找不到可用端口（%d ~ %d 都被占用）"
                % (FRONTEND_PORT, FRONTEND_PORT + FRONTEND_PORT_TRIES))
            return None, None

    try:
        # 绑 0.0.0.0：本机可用 127.0.0.1，局域网设备可用 LAN_HOST 访问同一套服务
        server = ThreadingHTTPServer((BIND_HOST, port), ProxyHandler)
    except OSError as e:
        die("前端服务启动失败：%s" % e)
        return None, None

    threading.Thread(target=server.serve_forever, daemon=True).start()
    log("前端联调服务已启动：http://%s:%d/" % (LAN_HOST, port))
    return server, port


def main():
    print("=" * 56)
    print("  7h 博客 · 一键启动")
    print("=" * 56)

    resolve_host()

    if not os.path.isdir(DIST_DIR):
        return die("找不到前端构建产物：%s" % DIST_DIR,
                   "先在 7h 目录执行 npm install && npm run build")

    missing = dist_assets_missing()
    if missing:
        warn("前端构建产物不完整，缺少：%s" % ", ".join(missing))
        warn("这样打开只会白屏 —— 需要重新 npm run build（源码齐全时）")

    if not os.path.isdir(BACKEND_DIR):
        return die("找不到后端目录：%s" % BACKEND_DIR)

    ensure_database()

    backend_proc = start_backend()
    if backend_proc is False:
        return 1

    server, port = start_frontend()
    if port is None:
        if backend_proc and backend_proc.poll() is None:
            subprocess.run(["taskkill", "/PID", str(backend_proc.pid), "/T", "/F"],
                           capture_output=True)
        return 1

    url = "http://%s:%d/" % (LAN_HOST, port)
    if not os.environ.get("NO_BROWSER"):
        log("正在打开浏览器：%s" % url)
        webbrowser.open(url)
    else:
        log("已跳过自动打开浏览器（NO_BROWSER=1）：%s" % url)

    print("\n" + "-" * 56)
    print("  访问地址：%s" % url)
    if LAN_HOST != "127.0.0.1":
        print("  本机也可用：http://127.0.0.1:%d/" % port)
    print("  接口地址：%s/api  （前端已反代，浏览器里无需直连）" % BACKEND)
    print("  账号：qinghe    密码：qinghe123      （青禾 / 管理员）")
    print("  账号：jingling  密码：jingling123    （景翎 / 管理员）")
    print("-" * 56)
    print("  后端日志：%s" % BACKEND_LOG)
    print("  上传文件持久化目录：%s" % UPLOAD_DATA_DIR)
    print("  换 IP：set BLOG_HOST=你的IP 后再运行 start-all.bat")
    print("  按 Ctrl+C 结束本脚本（后端会一起关掉）\n")

    try:
        while True:
            time.sleep(1)
            if backend_proc and backend_proc.poll() is not None:
                warn("后端进程已退出，日志尾部：")
                print(tail_file(BACKEND_LOG, 20))
                backend_proc = None
    except KeyboardInterrupt:
        log("正在关闭...")
        if server:
            server.shutdown()
        if backend_proc and backend_proc.poll() is None:
            subprocess.run(["taskkill", "/PID", str(backend_proc.pid), "/T", "/F"],
                           capture_output=True)
        log("已退出")
    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(errors="replace")
    except Exception:
        pass
    sys.exit(main())
