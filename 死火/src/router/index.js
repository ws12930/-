import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import ForgotView from '../views/ForgotView.vue'
import HomeView from '../views/HomeView.vue'
import BlogView from '../views/BlogView.vue'
import PostView from '../views/PostView.vue'
import EditorView from '../views/EditorView.vue'
import ProfileView from '../views/ProfileView.vue'
import UserView from '../views/UserView.vue'
import MessagesView from '../views/MessagesView.vue'

const routes = [
  { path: '/', name: 'login', component: LoginView },
  { path: '/register', name: 'register', component: RegisterView },
  { path: '/forgot', name: 'forgot', component: ForgotView },
  { path: '/home', name: 'home', component: HomeView, meta: { requiresAuth: true } },
  { path: '/blog', name: 'blog', component: BlogView },
  { path: '/posts/:slug', name: 'post', component: PostView },
  { path: '/mine', name: 'mine', component: BlogView, meta: { requiresAuth: true } },
  { path: '/editor', name: 'editor', component: EditorView, meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: ProfileView, meta: { requiresAuth: true } },
  { path: '/user/:id', name: 'user', component: UserView },
  { path: '/messages', name: 'messages', component: MessagesView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 简单守卫：未登录不能进 /home，已登录不再停留在登录页
router.beforeEach((to) => {
  const logged = !!localStorage.getItem('bn-user')
  if (to.meta.requiresAuth && !logged) {
    return { name: 'login' }
  }
  if (to.name === 'login' && logged) {
    return { name: 'home' }
  }
})

export default router
