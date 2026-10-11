<template>
  <div class="email-captcha-input">
    <label v-if="label">{{ label }}</label>
    <div class="captcha-input">
      <input
        v-model="inputValue"
        type="text"
        :placeholder="placeholder"
        :required="required"
        @input="handleInput"
      />
      <button
        type="button"
        @click="handleSendCode"
        :disabled="disabled || countdown > 0"
        class="send-code-btn"
      >
        {{ countdown > 0 ? `${countdown}秒` : buttonText }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入验证码'
  },
  required: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  buttonText: {
    type: String,
    default: '发送验证码'
  },
  countdownSeconds: {
    type: Number,
    default: 60
  }
})

const emit = defineEmits(['update:modelValue', 'input', 'send-code'])

const inputValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const countdown = ref(0)

const handleInput = (event) => {
  emit('input', event.target.value)
}

const handleSendCode = () => {
  emit('send-code')
  startCountdown()
}

const startCountdown = () => {
  countdown.value = props.countdownSeconds
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

defineExpose({
  startCountdown
})
</script>

<style scoped>
.email-captcha-input {
  margin-bottom: 20px;
}

.email-captcha-input label {
  display: block;
  margin-bottom: 8px;
  color: #fff;
  font-weight: 500;
  font-size: 14px;
  text-align: left;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.captcha-input {
  display: flex;
  gap: 10px;
}

.captcha-input input {
  flex: 1;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 5px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.captcha-input input:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.3);
}

.captcha-input input::placeholder {
  color: rgba(255, 255, 255, 0.7);
}

.send-code-btn {
  padding: 0 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.3s;
}

.send-code-btn:hover:not(:disabled) {
  background: #5568d3;
}

.send-code-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .email-captcha-input {
    margin-bottom: 16px;
  }

  .email-captcha-input label {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .captcha-input {
    flex-direction: column;
    gap: 8px;
  }

  .captcha-input input {
    padding: 10px;
    font-size: 13px;
  }

  .send-code-btn {
    padding: 10px;
    font-size: 13px;
  }
}
</style>
