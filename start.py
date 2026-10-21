#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""7h 博客一键启动脚本

双击 start-all.bat 即可，本脚本会依次完成：
  1. 拉起 Spring Boot 后端（8081），若已在运行则复用
  2. 启动前端联调服务（4173）：托管 7h/dist 静态资源 + 反代 /api、/uploads 到后端
  3. 等两个服务都就绪后自动打开浏览器

按 Ctrl+C 退出，会连带关闭后端进程。
"""
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
BACKEND_DIR = os.path.join(BASE_DIR, "springBootTest")
BACKEND = "http://127.0.0.1:8081"
FRONTEND_PORT = 4173
BACKEND_PORT = 8081
DB_PASSWORD = "123456"
PROXY_PREFIXES = ("/api/", "/uploads/")


def log(msg):
    print("[%s] %s" % (time.strftime("%H:%M:%S"), msg), flush=True)


def port_open(port, timeout=0.6):
    s = socket.socket()
    s.settimeout(timeout)
    try:
        return s.connect_ex(("127.0.0.1", port)) == 0
    finally:
        s.close()


def backend_alive():
    if not port_open(BACKEND_PORT):
        return False
    try:
        with urlopen(BACKEND + "/api/auth/ping", timeout=3) as resp:
            return resp.status == 200
    except Exception:
        return False


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
            self.wfile.write(payload)
        except URLError as e:
            msg = ("后端未响应：%s" % e.reason).encode("utf-8")
            self.send_response(502)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(msg)))
            self.end_headers()
            self.wfile.write(msg)

    def do_GET(self):
        self._proxy("GET") if self._is_proxy() else super().do_GET()

    def do_HEAD(self):
        self._proxy("HEAD") if self._is_proxy() else super().do_HEAD()

    def do_POST(self):
        self._proxy("POST") if self._is_proxy() else self.send_error(404)

    def do_PUT(self):
        self._proxy("PUT") if self._is_proxy() else self.send_error(404)

    def do_DELETE(self):
        self._proxy("DELETE") if self._is_proxy() else self.send_error(404)

    def end_headers(self):
        if not self._is_proxy():
            self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, fmt, *args):
        if self._is_proxy():
            sys.stderr.write("  %s - %s\n" % (self.address_string(), fmt % args))


# ------------------------------------------------------------------ 主流程
def start_backend():
    """启动 Spring Boot，返回 Popen；已在运行则返回 None"""
    if backend_alive():
        log("后端已在运行，直接复用 (http://127.0.0.1:%d)" % BACKEND_PORT)
        return None

    log("启动后端 Spring Boot (端口 %d)，首次启动需要几十秒..." % BACKEND_PORT)
    env = dict(os.environ)
    env["BLOG_DB_PASSWORD"] = DB_PASSWORD
    env.pop("SERVER__PORT", None)  # 避免被宽松绑定抢走 server.port
    creation = getattr(subprocess, "CREATE_NEW_CONSOLE", 0)
    proc = subprocess.Popen(
        ["mvnw.cmd", "spring-boot:run", "-Dspring-boot.run.arguments=--server.port=%d" % BACKEND_PORT],
        cwd=BACKEND_DIR,
        env=env,
        creationflags=creation,
    )
    for _ in range(120):
        if backend_alive():
            log("后端已就绪：http://127.0.0.1:%d/api" % BACKEND_PORT)
            return proc
        if proc.poll() is not None:
            log("后端进程意外退出，请查看后端窗口的报错")
            return None
        time.sleep(2)
    log("等待后端超时，请检查后端窗口日志")
    return proc


def main():
    print("=" * 56)
    print("  7h 博客 · 一键启动")
    print("=" * 56)

    if not os.path.isdir(DIST_DIR):
        input("\n[错误] 找不到前端构建产物：%s\n请先在 7h 目录执行 npm install && npm run build\n按回车退出..." % DIST_DIR)
        return 1

    backend_proc = start_backend()

    try:
        server = ThreadingHTTPServer(("127.0.0.1", FRONTEND_PORT), ProxyHandler)
    except OSError:
        log("端口 %d 已被占用，前端服务可能已在运行" % FRONTEND_PORT)
        server = None
    else:
        threading.Thread(target=server.serve_forever, daemon=True).start()
        log("前端联调服务已启动：http://127.0.0.1:%d/" % FRONTEND_PORT)

    url = "http://127.0.0.1:%d/" % FRONTEND_PORT
    time.sleep(0.5)
    log("正在打开浏览器：%s" % url)
    webbrowser.open(url)

    print("\n" + "-" * 56)
    print("  登录地址：%s" % url)
    print("  账号：qinghe    密码：qinghe123      （青禾）")
    print("  账号：jingling  密码：jingling123    （景翎）")
    print("-" * 56)
    print("  后端日志在独立窗口，关闭该窗口即可停止后端")
    print("  按 Ctrl+C 结束本脚本\n")

    try:
        while True:
            time.sleep(1)
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
    sys.exit(main())
