<template>
  <div class="auth-container">
    <div class="auth-box">
      <h2>{{ title }}</h2>
      <slot></slot>
      <div class="links">
        <slot name="links"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  }
})
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #808080 0%, #696969 100%);
  padding: 20px;
}

.auth-box {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8), rgba(255, 182, 193, 0.8));
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  width: 100%;
  max-width: 400px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #fff;
  font-size: 24px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.links {
  text-align: center;
  margin-top: 20px;
}

.links a {
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.links a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .auth-container {
    padding: 15px;
  }

  .auth-box {
    padding: 30px 20px;
  }

  h2 {
    font-size: 20px;
    margin-bottom: 20px;
  }

  .links a {
    font-size: 13px;
  }
}
</style>
