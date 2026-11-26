#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把青禾的三张静态立绘烘焙成桌宠动作帧序列（sprite sheet）。

为什么要在构建前先烘焙，而不是运行时用 CSS 变换硬凑：
  1. 桌宠的动作状态需要「帧」这个概念，运行时才能按状态切换素材、逐帧播放、
     单次播放后回到 idle；
  2. 美术以后要换成手绘帧或 Shimeji 格式的 PNG 序列时，只要保持同样的
     「横向条带 + 固定单元格 + 底边锚点」约定，播放器和状态机一行都不用改。

帧的生成方式参考了 GitHub 上 YuZhu412/zhizhiji-shimeji 的 gen_frames.py：
统一画布尺寸、底部对齐锚点、用垂直位移模拟重心起伏、用旋转模拟被拎起 / 下落。
区别是青禾的源图本身带 alpha，不需要去白底；这里额外做了挤压 / 拉伸（squash & stretch）
和左右走动两种倾斜。

动作集合参考 DSH（DeepSeek Harness）桌宠插件：
  * Practice019/dsh-kun-like-pet（MIT）：idle / runRight / runLeft / wave / jump /
    failed / waiting / working / review 共 9 状态，8 列精灵图、状态驱动切换；
  * sereinmono/dsh-desktop-pet（MIT）：语义状态 + 优先级折叠 + 瞬时状态回归 + 全局倍速；
  * PC2005-cloud/dsh-pet（MIT）：待机变体按权重衔接，点击有多种随机反应。
本脚本据此补出 wave / celebrate / think / failed，并把 idle / walk 等改成
「按归一化时间生成」，帧数可以自由提高而不改变动作时长（配合播放器提帧率）。

用法：
    pip install Pillow
    python scripts/gen_mascot_frames.py

输出：
    src/assets/mascot/anim/<人物>-<状态>.webp
    每张是横向排列的帧条，单元格固定 128x134，第 i 帧 = 裁剪 [i*cw, 0, (i+1)*cw, ch]

约定（播放器 MascotSprite.vue / 剪辑表 animations.js 依赖这三条）：
    * 单元格尺寸与帧数写在 src/mascot/animations.js，必须和这里保持一致；
    * 人物底边永远贴在单元格底边（GROUND），这样切换状态时脚不会跳；
    * 所有帧共用同一画布，不做自动裁剪，避免不同状态下人物大小忽大忽小。
"""
from __future__ import annotations

import math
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(HERE, "..", "src", "assets", "mascot")
OUT_DIR = os.path.join(SRC_DIR, "anim")

# 单元格 128x134：默认 112px 档是 1.14 倍下采样，最大 140px 档也只放大 1.09 倍，
# 视觉上没有明显损失；再往上加分辨率 WebP 体积会接近线性膨胀。
CELL_W, CELL_H = 128, 134
QUALITY = 72
# 画面里的「倾斜」交给 CSS 做（不会裁切），帧里只烘焙挤压 / 拉伸 / 上下起伏，
# 所以左右只需要留出横向拉伸的余量，人物可以占满 94% 的画布宽度。
# 注意：sx 的上限受 0.94 约束（0.94 * 1.045 ≈ 0.98），超过就会被 render() 报裁切。
BASE_FIT_W = 0.94
BASE_FIT_H = 0.86

# 三张表情：文件名后缀 -> 输出前缀
FACES = {
    "welcome": "mascot-welcome.webp",
    "nofight": "mascot-nofight.webp",
    "writing": "mascot-writing.webp",
}


def load_base(path: str) -> Image.Image:
    """读入源图并缩放到单元格内，返回底部对齐的底图。"""
    im = Image.open(path).convert("RGBA")
    max_w, max_h = CELL_W * BASE_FIT_W, CELL_H * BASE_FIT_H
    scale = min(max_w / im.width, max_h / im.height)
    return im.resize((max(1, round(im.width * scale)), max(1, round(im.height * scale))), Image.LANCZOS)


def render(base: Image.Image, sx: float = 1.0, sy: float = 1.0,
           angle: float = 0.0, ox: float = 0.0, oy: float = 0.0) -> Image.Image:
    """
    以「底边中点」为锚点渲染一帧。

    sx / sy   挤压拉伸，底边不动（所以 sy<1 是蹲，sy>1 是拉长）
    angle     绕锚点旋转的角度，正值逆时针（控制在 1 度内，更大的倾斜交给 CSS）
    ox / oy   整帧在单元格里的偏移（oy 为负 = 抬高）
    """
    im = base
    if abs(sx - 1) > 1e-4 or abs(sy - 1) > 1e-4:
        im = im.resize((max(1, round(im.width * sx)), max(1, round(im.height * sy))), Image.LANCZOS)

    # 三倍单元格的舞台：锚点放在 (1.5cw, 2ch)，旋转时四角有足够余量不被裁掉
    stage = Image.new("RGBA", (CELL_W * 3, CELL_H * 3), (0, 0, 0, 0))
    pivot = (CELL_W * 1.5, CELL_H * 2.0)
    stage.paste(im, (round(pivot[0] - im.width / 2), round(pivot[1] - im.height)), im)

    if abs(angle) > 1e-4:
        stage = stage.rotate(angle, resample=Image.BICUBIC, center=pivot)

    left = round(pivot[0] - CELL_W / 2 - ox)
    top = round(pivot[1] - CELL_H - oy)
    frame = stage.crop((left, top, left + CELL_W, top + CELL_H))

    bbox = frame.getbbox()
    # 底边贴边是设计如此（脚踩在 GROUND 上），只检查左 / 上 / 右三边，
    # 贴边说明姿态把人物顶出了单元格，播放时会看到硬切口。
    if bbox and (bbox[0] == 0 or bbox[1] == 0 or bbox[2] == CELL_W):
        print("    ! 注意：有一帧贴到单元格边缘，可能会被裁切")
    return frame


# ---------------------------------------------------------------------------
# 动作生成函数。统一签名 (i, n) -> render 参数字典。
# 全部按归一化时间 t 计算，帧数 n 只影响采样密度，不改变动作时长与幅度，
# 因此可以单独提高帧率让动作更顺，不用重调手感。
# ---------------------------------------------------------------------------

def breath_idle(i: int, n: int) -> dict:
    t = i / n
    s = math.sin(2 * math.pi * t)
    return dict(sx=1 - 0.012 * s, sy=1 + 0.028 * s,
                angle=0.8 * math.sin(2 * math.pi * t + 0.6), oy=-2.6 * s)


def walk(i: int, n: int, direction: int) -> dict:
    # 两步一个循环：重心起伏两次，落地帧略微压扁；angle 只给一点，大倾斜交给 CSS
    t = i / n
    phase = 4 * math.pi * t
    bob = -3.0 - 3.0 * math.sin(phase - math.pi / 2)
    sy = 1.012 + 0.018 * math.sin(phase)
    ang = direction * 0.6 + 0.9 * math.sin(phase + 0.5)
    return dict(sx=1.006, sy=sy, angle=ang, oy=bob * 0.85)


def jump(i: int, n: int) -> dict:
    # 蓄力 -> 起跳拉伸 -> 滞空回落 -> 触地缓冲 -> 回正
    t = i / max(1, n - 1)
    if t < 0.2:
        k = t / 0.2
        sx, sy = 1.0 + 0.04 * k, 1.0 - 0.12 * k
    elif t < 0.55:
        k = (t - 0.2) / 0.35
        sx, sy = 1.04 - 0.09 * k, 0.88 + 0.21 * k
    elif t < 0.8:
        k = (t - 0.55) / 0.25
        sx, sy = 0.95 + 0.05 * k, 1.09 - 0.09 * k
    else:
        k = (t - 0.8) / 0.2
        sx, sy = 1.0 + 0.03 * (1 - k), 1.0 - 0.08 * (1 - k)
    return dict(sx=sx, sy=sy, angle=0.7 * math.sin(2 * math.pi * t), oy=0)


def drag(i: int, n: int) -> dict:
    # 被拎住兜帽，像钟摆一样来回晃
    t = i / n
    return dict(sx=0.995, sy=1.035, angle=1.0 * math.sin(2 * math.pi * t),
                oy=-1.0 - 0.6 * math.sin(4 * math.pi * t))


def land(i: int, n: int) -> dict:
    # 阻尼振荡：触地先压扁，再过冲，收敛回正
    t = i / max(1, n - 1)
    damp = math.exp(-3.2 * t)
    wave = math.cos(2 * math.pi * 1.2 * t) * damp
    return dict(sx=1 + 0.045 * wave, sy=1 - 0.13 * wave, angle=0.0, oy=0)


def sleep(i: int, n: int) -> dict:
    t = i / n
    s = math.sin(2 * math.pi * t)
    return dict(sx=1 - 0.004 * s, sy=1 + 0.028 * s, angle=0.3 * s, oy=-2.4 * s)


def wave(i: int, n: int) -> dict:
    # 招手（对应 Kun Like Pet 的 wave）：探身 + 抬手式起伏，一次性动作
    keys = [
        (1.000, 1.010, 0.8, 1.5, -1.5),
        (0.998, 1.030, -0.6, 2.0, -3.2),
        (1.002, 0.995, 0.4, -1.5, -0.8),
        (1.000, 1.000, 0.0, 0.0, 0.0),
    ]
    sx, sy, ang, ox, oy = keys[i % len(keys)]
    return dict(sx=sx, sy=sy, angle=ang, ox=ox, oy=oy)


def celebrate(i: int, n: int) -> dict:
    # 庆祝（对应 celebrating / SUCCESS）：连着蹦两下，最后回正
    keys = [
        (1.020, 0.94, 0.0, 0.0),
        (0.965, 1.08, 0.6, -4.5),
        (0.985, 1.04, -0.4, -2.0),
        (1.025, 0.93, 0.0, 0.0),
        (0.970, 1.07, 0.5, -3.5),
        (1.000, 1.00, 0.0, 0.0),
    ]
    sx, sy, ang, oy = keys[i % len(keys)]
    return dict(sx=sx, sy=sy, angle=ang, oy=oy)


def think(i: int, n: int) -> dict:
    # 思考 / 摸鱼（对应 review / THINKING）：固定歪头 + 慢呼吸，循环播放
    t = i / n
    s = math.sin(2 * math.pi * t)
    return dict(sx=1 - 0.006 * s, sy=1 + 0.018 * s, angle=0.7 + 0.3 * s, oy=-1.2 * s)


def failed(i: int, n: int) -> dict:
    # 沮丧（对应 ERROR）：压扁下沉，再慢慢回一点，一次性动作
    keys = [
        (1.020, 0.94, 0.0),
        (1.035, 0.87, -0.4),
        (1.030, 0.89, -0.3),
        (1.015, 0.94, -0.2),
        (1.000, 1.00, 0.0),
    ]
    sx, sy, ang = keys[i % len(keys)]
    return dict(sx=sx, sy=sy, angle=ang, oy=0)


# 状态 -> (帧数, 生成函数)。播放器 / animations.js 里的帧数、fps 必须与此一致。
# 帧数与 fps 同比例提高，动作总时长（frames / fps）保持和旧版一致，见 animations.js。
CLIPS = {
    "idle":      (10, breath_idle),
    "sleep":     (6, sleep),
    "walk-r":    (8, lambda i, n: walk(i, n, +1)),
    "walk-l":    (8, lambda i, n: walk(i, n, -1)),
    "jump":      (10, jump),
    "drag":      (6, drag),
    "land":      (5, land),
    # ↓ 参考 DSH 桌宠插件补齐的状态
    "wave":      (4, wave),
    "celebrate": (6, celebrate),
    "think":     (5, think),
    "failed":    (5, failed),
}


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    total_bytes = 0
    for key, filename in FACES.items():
        src = os.path.join(SRC_DIR, filename)
        if not os.path.exists(src):
            raise SystemExit("缺少源图：%s" % src)
        base = load_base(src)
        print("[%s] 底图 %dx%d" % (key, base.width, base.height))
        for state, (count, fn) in CLIPS.items():
            sheet = Image.new("RGBA", (CELL_W * count, CELL_H), (0, 0, 0, 0))
            for i in range(count):
                sheet.paste(render(base, **fn(i, count)), (i * CELL_W, 0))
            out = os.path.join(OUT_DIR, "%s-%s.webp" % (key, state))
            sheet.save(out, "WEBP", quality=QUALITY, method=6)
            size = os.path.getsize(out)
            total_bytes += size
            print("  ✓ %-22s %2d 帧  %6.1f KB" % (os.path.basename(out), count, size / 1024))
    print("\n合计 %.1f KB，共 %d 张帧条 → %s"
          % (total_bytes / 1024, len(FACES) * len(CLIPS), OUT_DIR))


if __name__ == "__main__":
    main()
