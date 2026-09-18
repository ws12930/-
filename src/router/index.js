import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'

const routes = [
  { path: '/', name: 'login', component: LoginView }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
