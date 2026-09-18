import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import StoryView from '../views/StoryView.vue'
import CGView from '../views/CGView.vue'
import EndingView from '../views/EndingView.vue'

const routes = [
  { path: '/', name: 'login', component: LoginView },
  { path: '/home', name: 'home', component: HomeView },
  { path: '/story', name: 'story', component: StoryView },
  { path: '/cg', name: 'cg', component: CGView },
  { path: '/endings', name: 'endings', component: EndingView }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
