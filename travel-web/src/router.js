import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Home from './views/Home.vue'
import Detail from './views/Detail.vue'
import Chat from './views/Chat.vue'
import Hotel from './views/Hotel.vue'
import HotelDetail from './views/HotelDetail.vue'
import Pay from './views/Pay.vue'
import SpotDetail from './views/SpotDetail.vue'
import City from './views/City.vue'
import Flight from './views/Flight.vue'
import FlightResult from './views/FlightResult.vue'
import Train from './views/Train.vue'
import TrainResult from './views/TrainResult.vue'
import Travel from './views/Travel.vue'
import Plan from './views/Plan.vue'
import Orders from './views/Orders.vue'
import Notices from './views/Notices.vue'
import MyTrips from './views/MyTrips.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/home', component: Home, meta: { requiresAuth: true } },
    { path: '/chat', component: Chat, meta: { requiresAuth: true } },
    { path: '/detail', component: Detail, meta: { requiresAuth: true } },
    { path: '/hotel', component: Hotel, meta: { requiresAuth: true } },
    { path: '/hotel-detail', component: HotelDetail, meta: { requiresAuth: true } },
    { path: '/pay', component: Pay, meta: { requiresAuth: true } },
    { path: '/spot-detail', component: SpotDetail, meta: { requiresAuth: true } },
    { path: '/city', component: City, meta: { requiresAuth: true } },
    { path: '/flight', component: Flight, meta: { requiresAuth: true } },
    { path: '/flight-result', component: FlightResult, meta: { requiresAuth: true } },
    { path: '/train', component: Train, meta: { requiresAuth: true } },
    { path: '/train-result', component: TrainResult, meta: { requiresAuth: true } },
    { path: '/travel', component: Travel, meta: { requiresAuth: true } },
    { path: '/plan', component: Plan, meta: { requiresAuth: true } },
    { path: '/orders', component: Orders, meta: { requiresAuth: true } },
    { path: '/notices', component: Notices, meta: { requiresAuth: true } },
    { path: '/my-trips', component: MyTrips, meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
