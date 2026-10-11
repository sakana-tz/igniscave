<template>
  <button
    type="submit"
    :disabled="disabled || loading"
    class="submit-btn"
  >
    {{ loading ? loadingText : buttonText }}
  </button>
</template>

<script setup>
defineProps({
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  buttonText: {
    type: String,
    default: '提交'
  },
  loadingText: {
    type: String,
    default: '提交中...'
  }
})
</script>

<style scoped>
.submit-btn {
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 10px;
  transition: all 0.3s;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.submit-btn:hover {
  background: rgba(255, 255, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.8);
}

.submit-btn:disabled {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
}
</style>
