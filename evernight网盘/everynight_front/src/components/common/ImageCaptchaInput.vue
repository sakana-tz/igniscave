<template>
  <div class="image-captcha-input">
    <label v-if="label">{{ label }}</label>
    <div class="captcha-input">
      <input
        v-model="inputValue"
        type="text"
        :placeholder="placeholder"
        :required="required"
        @input="handleInput"
      />
      <img
        v-if="captchaImage"
        :src="captchaImage"
        @click="handleRefresh"
        class="captcha-img"
        alt="验证码"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

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
  captchaImage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'input', 'refresh'])

const inputValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const handleInput = (event) => {
  emit('input', event.target.value)
}

const handleRefresh = () => {
  emit('refresh')
}
</script>

<style scoped>
.image-captcha-input {
  margin-bottom: 20px;
}

.image-captcha-input label {
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

.captcha-img {
  width: 120px;
  height: 42px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 5px;
}

.captcha-img:hover {
  opacity: 0.8;
}
</style>
