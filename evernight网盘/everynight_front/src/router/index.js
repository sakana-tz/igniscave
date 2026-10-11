import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../utils/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/share',
    name: 'Share',
    component: () => import('../views/Share.vue')
  },
  {
    path: '/square',
    name: 'Square',
    component: () => import('../views/Square.vue')
  },
  {
    path: '/square/post/:postId',
    name: 'ForumDetail',
    component: () => import('../views/ForumDetail.vue')
  },
  {
    path: '/square/create',
    name: 'ForumCreate',
    component: () => import('../views/ForumEditor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/square/edit/:postId',
    name: 'ForumEdit',
    component: () => import('../views/ForumEditor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/files',
    name: 'Files',
    component: () => import('../views/Files.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('../views/Users.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('../views/About.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/login')
  } else {
    next()
  }
})

export default router
