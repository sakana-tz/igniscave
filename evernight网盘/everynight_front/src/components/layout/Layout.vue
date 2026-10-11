<template>
  <div class="layout">
    <Header />
    <main class="main-content">
      <router-view />
    </main>
    <footer class="footer">
      <p>&copy; 2026 Evernight网盘. We were never apart.</p>
    </footer>
  </div>
</template>

<script setup>
import Header from './Header.vue'
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding-top: 60px;
  min-height: calc(100vh - 120px);
}

.footer {
  background: #f5f5f5;
  padding: 20px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

@media (max-width: 768px) {
  .main-content {
    padding-top: 60px;
  }

  .footer {
    padding: 15px;
    font-size: 12px;
  }
}
</style>
