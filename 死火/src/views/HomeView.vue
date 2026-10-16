<template>
  <div class="home">
    <div class="auth-bg left"></div>
    <div class="auth-bg right"></div>

    <!-- 阵营立绘：保留氛围装饰，降低存在感避免干扰阅读 -->
    <div class="home-cut naruto" :class="{ active: faction === 'ninja' }">
      <img :src="narutoImg" alt="" />
    </div>
    <div class="home-cut ichigo" :class="{ active: faction === 'shinigami' }">
      <img :src="ichigoImg" alt="" />
    </div>

    <!-- ===== 顶部横条：logo + 搜索 + 用户功能 ===== -->
    <TopBar
      v-model:keyword="keyword"
      v-model:category-id="categoryId"
      active="home"
      show-search
      @search="loadPosts"
    />

    <!-- ===== 三栏主体：首屏轮播栏 + 左侧分类导航 | 中间信息流 | 右侧个人/热门 ===== -->
    <div class="layout">
      <!-- 首屏轮播栏：独占整行，位于三栏之上 -->
      <div class="home-banner">
        <BannerCarousel />
      </div>

      <!-- 左侧栏：导航 + 分类 -->
      <aside class="side-left">
        <nav class="lnav">
          <button class="lnav-item active">
            <span class="lnav-ico ico-home"></span>首页
          </button>
          <button class="lnav-item" @click="goProfile">
            <span class="lnav-ico ico-user"></span>个人资料
          </button>
        </nav>

        <div class="lnav-group">
          <p class="lnav-title">文章分类</p>
          <button
            class="lnav-item cat"
            :class="{ on: !categoryId }"
            @click="pickCategory('')"
          >
            <span class="cat-chip all"></span>全部文章
          </button>
          <button
            v-for="(c, i) in categories"
            :key="c.id"
            class="lnav-item cat"
            :class="{ on: categoryId === String(c.id) }"
            @click="pickCategory(String(c.id))"
          >
            <span class="cat-chip" :style="{ background: catColor(i) }"></span>{{ c.name }}
          </button>
        </div>
      </aside>

      <!-- 中间信息流 -->
      <main class="feed">
        <p v-if="error" class="auth-alert err">{{ error }}</p>
        <p v-if="loading" class="feed-empty">加载中…</p>

        <template v-else>
          <article
            v-for="post in posts"
            :key="post.id"
            class="panel post-card"
            @click="openPost(post)"
          >
            <!-- 作者行 -->
            <div class="pc-head">
              <UserAvatar
                :avatar="post.authorAvatar"
                :name="post.authorName"
                :faction="post.authorFaction"
                :size="34"
              />
              <div class="pc-author">
                <span class="pc-name" @click.stop="openAuthor(post)">{{ post.authorName }}</span>
                <span class="pc-sub">
                  {{ post.categoryName || '未分类' }} · {{ post._date }}
                </span>
              </div>
              <span class="pc-faction" :class="post.authorFaction">
                {{ post.authorFaction === 'shinigami' ? '死神' : '忍者' }}
              </span>
            </div>

            <!-- 标题 + 摘要 -->
            <h3 class="pc-title">{{ post.title }}</h3>
            <p class="pc-summary">{{ post.summary || '（暂无摘要）' }}</p>

            <!-- 标签 -->
            <div v-if="(post.tags || []).length" class="pc-tags">
              <span v-for="t in post.tags" :key="t.id" class="tag-chip">{{ t.name }}</span>
            </div>

            <!-- 底部操作栏 -->
            <div class="pc-foot" @click.stop>
              <button class="pc-act" :class="{ on: post.liked }" @click="like(post)">
                ♥ {{ post.likeCount || 0 }}
              </button>
              <span class="pc-act-sep"></span>
              <button class="pc-act" @click="openPost(post)">
                ✦ 评论 {{ post.commentCount || 0 }}
              </button>
              <span class="pc-act-sep"></span>
              <span class="pc-act static">👁 {{ post.viewCount || 0 }} 阅读</span>
              <button
                v-if="isMine(post)"
                class="pc-act del"
                @click="removePost(post)"
              >
                ✕ 删除
              </button>
              <span class="pc-time">{{ post._time }}</span>
            </div>
          </article>

          <p v-if="!posts.length && !error" class="feed-empty">
            {{ keyword || categoryId ? '没有匹配的文章，试试其它关键词' : '还没有文章，去“写文章”发布第一篇吧' }}
          </p>
          <p v-if="truncated" class="feed-tip">
            仅展示最近 {{ PAGE_SIZE }} 篇文章，更多内容请到“我的文章”查看
          </p>
        </template>
      </main>

      <!-- 右侧栏：个人卡 + 热门榜 + 宣传图 -->
      <aside class="side-right">
        <!-- 个人信息卡 -->
        <div class="panel rcard user-card">
          <div class="uc-top">
            <span class="uc-avatar" :class="faction">
              {{ (user.nickname || user.username || '?').slice(0, 1) }}
            </span>
            <div class="uc-info">
              <p class="uc-name">{{ user.nickname || user.username }}</p>
              <span class="uc-faction" :class="faction">
                {{ faction === 'shinigami' ? '死神阵营' : '忍者阵营' }}
              </span>
            </div>
          </div>
          <div class="uc-actions">
            <button class="mini-btn uc-edit" @click="goProfile">编辑资料</button>
          </div>
        </div>

        <!-- 热门榜 -->
        <div class="panel rcard hot-card">
          <p class="rc-title"><span class="flame">🔥</span> 最热文章</p>
          <ol class="hot-list">
            <li
              v-for="(p, i) in hotPosts"
              :key="p.id"
              class="hot-item"
              @click="openPost(p)"
            >
              <span class="hot-no" :class="`no-${i + 1}`">{{ i + 1 }}</span>
              <span class="hot-title">{{ p.title }}</span>
              <span class="hot-count">{{ p.viewCount || 0 }}</span>
            </li>
          </ol>
          <p v-if="!hotPosts.length" class="hot-empty">暂无数据</p>
        </div>

        <!-- 双阵营宣传卡 -->
        <div class="panel rcard promo-card">
          <p class="promo-title grad-text">BLEACH × NARUTO</p>
          <p class="promo-sub">死神对火影 · 双阵营博客</p>
          <div class="promo-sides">
            <span class="ps ninja">忍者</span>
            <span class="ps vs">VS</span>
            <span class="ps shinigami">死神</span>
          </div>
        </div>
      </aside>
    </div>

    <p v-if="hint" class="auth-toast">{{ hint }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { listPosts, listCategories, deletePost } from '../api/blog'
import { toggleLike } from '../api/social'
import UserAvatar from '../components/UserAvatar.vue'
import TopBar from '../components/TopBar.vue'
import BannerCarousel from '../components/BannerCarousel.vue'
import { bgAutoPlay } from '../utils/bgState'
import narutoImg from '../assets/naruto_cut.png'
import ichigoImg from '../assets/ichigo_cut.png'

const PAGE_SIZE = 50

const router = useRouter()
const user = ref({})
const hint = ref('')
const posts = ref([])
const categories = ref([])
const keyword = ref('')
const categoryId = ref('')
const loading = ref(false)
const error = ref('')
const total = ref(0)

const faction = computed(() => (user.value.faction === 'shinigami' ? 'shinigami' : 'ninja'))
const truncated = computed(() => total.value > posts.value.length)

/** 热门榜：按阅读量取前 5 */
const hotPosts = computed(() =>
  [...posts.value].sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0)).slice(0, 5)
)

/** 分类图标配色 */
const CAT_COLORS = [
  'linear-gradient(135deg, #ff8c1a, #ffd24a)',
  'linear-gradient(135deg, #00e5ff, #0080ff)',
  'linear-gradient(135deg, #ff6b8a, #ff3d6e)',
  'linear-gradient(135deg, #8cff9e, #00c853)',
  'linear-gradient(135deg, #c792ff, #7c4dff)',
  'linear-gradient(135deg, #ffd24a, #ff8c1a)'
]
function catColor(i) {
  return CAT_COLORS[i % CAT_COLORS.length]
}

function pad(n) {
  return String(n).padStart(2, '0')
}

function fmtDate(raw) {
  const d = new Date(raw)
  if (!raw || Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

onMounted(() => {
  try {
    const saved = localStorage.getItem('bn-user')
    if (!saved) {
      router.replace({ name: 'login' })
      return
    }
    user.value = JSON.parse(saved)
  } catch (e) {
    router.replace({ name: 'login' })
    return
  }
  // 首屏轮播栏接管视觉焦点，背景轮播定格，避免两个轮播同时切换
  bgAutoPlay.value = false
  loadTaxonomy()
  loadPosts()
})

let hintTimer = null
function toast(msg) {
  hint.value = msg
  clearTimeout(hintTimer)
  hintTimer = setTimeout(() => (hint.value = ''), 2000)
}
onBeforeUnmount(() => {
  clearTimeout(hintTimer)
  bgAutoPlay.value = true
})

async function loadTaxonomy() {
  try {
    const res = await listCategories()
    categories.value = res.data || []
  } catch (e) {
    // 分类加载失败不阻塞文章列表
  }
}

async function loadPosts() {
  loading.value = true
  error.value = ''
  try {
    const params = { current: 1, size: PAGE_SIZE }
    if (keyword.value) params.keyword = keyword.value
    if (categoryId.value) params.categoryId = categoryId.value
    const res = await listPosts(params)
    posts.value = (res.data?.records || []).map((p) => ({
      ...p,
      _date: fmtDate(p.publishedAt || p.createdAt)
    }))
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    error.value = e.message || '文章加载失败'
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function pickCategory(id) {
  categoryId.value = id
  loadPosts()
}

function openPost(post) {
  if (!post.slug) {
    toast('该文章暂不可访问')
    return
  }
  router.push({ name: 'post', params: { slug: post.slug } })
}

function openAuthor(post) {
  if (!post.authorId) return
  router.push({ name: 'user', params: { id: post.authorId } })
}

/** 是否为自己的帖子（可删除） */
function isMine(post) {
  return post.authorId != null && String(post.authorId) === String(user.value.id)
}

async function removePost(post) {
  if (!window.confirm(`确定删除文章《${post.title}》吗？删除后不可恢复。`)) return
  try {
    await deletePost(post.id)
    toast('文章已删除')
    loadPosts()
  } catch (e) {
    toast(e.message || '删除失败')
  }
}

async function like(post) {
  try {
    const res = await toggleLike(post.id)
    post.likeCount = res.data.likeCount
    post.liked = res.data.liked
  } catch (e) {
    toast(e.message || '操作失败')
  }
}

function goProfile() {
  router.push({ name: 'profile' })
}
</script>

<style scoped>
.home {
  height: 100%;
  position: relative;
}

/* ===== 阵营立绘：降透明度作氛围装饰 ===== */
.home-cut {
  position: fixed;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.16;
  transition: opacity 0.6s;
}
.home-cut.naruto { left: -30px; }
.home-cut.ichigo { right: -30px; }
.home-cut img { width: clamp(200px, 22vw, 360px); display: block; }
.home-cut.active { opacity: 0.3; }

/* ===== 顶部横条（样式在 TopBar 组件内）===== */

/* ===== 三栏布局 ===== */
.layout {
  position: relative;
  z-index: 2;
  max-width: 1280px;
  margin: 0 auto;
  padding: 78px 20px 40px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  align-content: flex-start;
  gap: 18px;
  height: 100%;
  overflow-y: auto;
}

/* 首屏轮播栏：占满整行，强制三栏换到下一行 */
.home-banner {
  flex: none;
  width: 100%;
  position: relative;
  z-index: 2;
}

/* ---- 左侧栏 ---- */
.side-left {
  flex: none;
  width: 172px;
  position: sticky;
  top: 78px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.lnav,
.lnav-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.lnav-title {
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--c-dim);
  padding: 0 12px 6px;
}
.lnav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--c-dim);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: color 0.2s, background 0.2s;
  position: relative;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.lnav-item:hover { color: var(--c-text); background: rgba(255, 255, 255, 0.05); }
.lnav-item.active,
.lnav-item.on {
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 3px 0 0 var(--c-ninja), inset -3px 0 0 var(--c-shinigami);
}
.lnav-badge {
  margin-left: auto;
  min-width: 16px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
  border-radius: 8px;
  background: #ff5c5c;
  color: #fff;
  font-size: 10px;
  text-align: center;
}

.lnav-ico {
  width: 16px;
  height: 16px;
  flex: none;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.18);
  position: relative;
}
.ico-home::after {
  content: '';
  position: absolute;
  inset: 4px 3px 3px;
  background: linear-gradient(135deg, #ffd24a, #ff6a00);
  clip-path: polygon(50% 0, 100% 40%, 100% 100%, 0 100%, 0 40%);
}
.ico-edit::after {
  content: '';
  position: absolute;
  inset: 3px;
  background: linear-gradient(135deg, #00e5ff, #0080ff);
  clip-path: polygon(0 100%, 20% 70%, 80% 10%, 100% 30%, 40% 90%);
}
.ico-mine::after {
  content: '';
  position: absolute;
  inset: 3px 5px;
  background: linear-gradient(135deg, #8cff9e, #00c853);
  border-radius: 1px;
}
.ico-msg::after {
  content: '';
  position: absolute;
  inset: 3px;
  background: linear-gradient(135deg, #c792ff, #7c4dff);
  clip-path: polygon(0 0, 100% 0, 100% 75%, 40% 75%, 15% 100%, 15% 75%, 0 75%);
}
.ico-user::after {
  content: '';
  position: absolute;
  inset: 2px 4px;
  background: linear-gradient(135deg, #ffd24a, #ff3d6e);
  border-radius: 50% 50% 40% 40%;
}

.cat-chip {
  width: 18px;
  height: 18px;
  flex: none;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.18);
}
.cat-chip.all { background: linear-gradient(135deg, #ff8c1a, #0080ff); }

/* ---- 中间信息流 ---- */
.feed {
  flex: 1;
  min-width: 0;
  max-width: 660px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}
.post-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.22);
  box-shadow: 0 6px 22px rgba(255, 140, 26, 0.14), 0 6px 22px rgba(0, 128, 255, 0.14);
}

.pc-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px 0;
}
.pc-author {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pc-name {
  font-size: 13px;
  color: var(--c-text);
  letter-spacing: 1px;
  cursor: pointer;
}
.pc-name:hover { text-decoration: underline; }
.pc-sub { font-size: 11px; color: var(--c-dim); }
.pc-faction {
  flex: none;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 999px;
  letter-spacing: 1px;
}
.pc-faction.ninja {
  color: var(--c-ninja);
  border: 1px solid rgba(255, 140, 26, 0.4);
  background: rgba(255, 140, 26, 0.1);
}
.pc-faction.shinigami {
  color: var(--c-shinigami);
  border: 1px solid rgba(0, 229, 255, 0.35);
  background: rgba(0, 229, 255, 0.08);
}

.pc-title {
  padding: 10px 18px 0;
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 1px;
  color: var(--c-text);
  line-height: 1.4;
}
.post-card:hover .pc-title { color: var(--c-ninja); }

.pc-summary {
  padding: 6px 18px 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--c-dim);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pc-tags {
  padding: 8px 18px 0;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.pc-foot {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 9px 14px;
  background: rgba(255, 255, 255, 0.03);
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}
.pc-act {
  padding: 4px 10px;
  font-size: 12px;
  color: var(--c-dim);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}
.pc-act:hover { color: var(--c-text); background: rgba(255, 255, 255, 0.06); }
.pc-act.on { color: #ff6b8a; }
.pc-act.del { margin-left: 8px; }
.pc-act.del:hover { color: #ff6b6b; background: rgba(255, 92, 92, 0.1); }
.pc-act.static { cursor: default; }
.pc-act-sep {
  width: 1px;
  height: 12px;
  background: rgba(255, 255, 255, 0.12);
}
.pc-time {
  margin-left: auto;
  font-size: 11px;
  color: var(--c-dim);
  opacity: 0.7;
}

.tag-chip {
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 12px;
  color: var(--c-shinigami);
  border: 1px solid rgba(0, 229, 255, 0.35);
  background: rgba(0, 229, 255, 0.08);
}

.feed-empty {
  text-align: center;
  font-size: 14px;
  color: var(--c-dim);
  padding: 60px 0;
  letter-spacing: 1px;
}
.feed-tip {
  text-align: center;
  font-size: 12px;
  color: var(--c-dim);
  padding: 4px 0 10px;
}

/* ---- 右侧栏 ---- */
.side-right {
  flex: none;
  width: 272px;
  position: sticky;
  top: 78px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.rcard { padding: 16px; }
.rc-title {
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 2px;
  color: var(--c-text);
  margin-bottom: 12px;
}
.rc-title .flame { font-size: 14px; }

/* 个人卡 */
.uc-top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.uc-avatar {
  width: 46px;
  height: 46px;
  flex: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 20px;
  color: #0a0a12;
}
.uc-avatar.ninja { background: linear-gradient(135deg, #ffd24a, #ff6a00); box-shadow: 0 0 18px rgba(255, 140, 26, 0.5); }
.uc-avatar.shinigami { background: linear-gradient(135deg, #b9f4ff, #0090ff); box-shadow: 0 0 18px rgba(0, 229, 255, 0.5); }
.uc-name {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--c-text);
  letter-spacing: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.uc-faction {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 999px;
}
.uc-faction.ninja {
  color: var(--c-ninja);
  border: 1px solid rgba(255, 140, 26, 0.4);
  background: rgba(255, 140, 26, 0.1);
}
.uc-faction.shinigami {
  color: var(--c-shinigami);
  border: 1px solid rgba(0, 229, 255, 0.35);
  background: rgba(0, 229, 255, 0.08);
}
.uc-actions {
  margin-top: 14px;
  display: flex;
  gap: 8px;
}
/* 次级按钮样式收敛到 main.css 的 .mini-btn，此处不再重复定义 */
.uc-edit { flex: 1; }

/* 热门榜 */
.hot-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}
.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}
.hot-item:hover { background: rgba(255, 255, 255, 0.05); }
.hot-item + .hot-item { border-top: 1px dashed rgba(255, 255, 255, 0.07); }
.hot-no {
  flex: none;
  width: 18px;
  text-align: center;
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--c-dim);
}
.hot-no.no-1 { color: #ff4d4d; font-weight: bold; }
.hot-no.no-2 { color: #ff8c1a; font-weight: bold; }
.hot-no.no-3 { color: #ffd24a; font-weight: bold; }
.hot-title {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  color: var(--c-dim);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hot-item:hover .hot-title { color: var(--c-text); }
.hot-count {
  flex: none;
  font-size: 11px;
  color: var(--c-dim);
  opacity: 0.7;
}
.hot-empty {
  text-align: center;
  font-size: 12px;
  color: var(--c-dim);
  padding: 14px 0;
}

/* 宣传卡 */
.promo-card {
  text-align: center;
  background:
    radial-gradient(ellipse at 15% 100%, rgba(255, 140, 26, 0.22), transparent 55%),
    radial-gradient(ellipse at 85% 100%, rgba(0, 128, 255, 0.22), transparent 55%),
    var(--c-panel);
}
.promo-title {
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 2px;
}
.promo-sub {
  margin-top: 4px;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--c-dim);
}
.promo-sides {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.ps {
  font-size: 13px;
  letter-spacing: 3px;
  padding: 5px 16px;
  border-radius: 6px;
}
.ps.ninja {
  color: #ffb266;
  border: 1px solid rgba(255, 140, 26, 0.5);
  background: rgba(255, 140, 26, 0.12);
  text-shadow: 0 0 10px rgba(255, 140, 26, 0.6);
}
.ps.shinigami {
  color: #7ceaff;
  border: 1px solid rgba(0, 229, 255, 0.45);
  background: rgba(0, 229, 255, 0.1);
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.6);
}
.ps.vs {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--c-text);
  padding: 0;
  border: none;
  background: none;
}

/* ===== 响应式 ===== */
@media (max-width: 1120px) {
  .side-right { display: none; }
}
@media (max-width: 860px) {
  .side-left { display: none; }
}
@media (max-width: 700px) {
  .layout { padding: 128px 12px 32px; }
  .pc-head, .pc-title, .pc-summary, .pc-tags { padding-left: 14px; padding-right: 14px; }
  .pc-foot { padding: 8px 8px; }
}
</style>
