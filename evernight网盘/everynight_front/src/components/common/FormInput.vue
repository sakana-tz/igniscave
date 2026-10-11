<template>
  <div class="form-input">
    <label v-if="label">{{ label }}</label>
    <input
      v-model="inputValue"
      :type="type"
      :placeholder="placeholder"
      :required="required"
      :minlength="minlength"
      :maxlength="maxlength"
      @input="handleInput"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  required: {
    type: Boolean,
    default: false
  },
  minlength: {
    type: Number,
    default: undefined
  },
  maxlength: {
    type: Number,
    default: undefined
  }
})

const emit = defineEmits(['update:modelValue', 'input'])

const inputValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const handleInput = (event) => {
  emit('input', event.target.value)
}
</script>

<style scoped>
.form-input {
  margin-bottom: 20px;
}

.form-input label {
  display: block;
  margin-bottom: 8px;
  color: #fff;
  font-weight: 500;
  font-size: 14px;
  text-align: left;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.form-input input {
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 5px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.form-input input:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.3);
}

.form-input input::placeholder {
  color: rgba(255, 255, 255, 0.7);
}

@media (max-width: 480px) {
  .form-input {
    margin-bottom: 16px;
  }

  .form-input label {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .form-input input {
    padding: 10px;
    font-size: 13px;
  }
}
</style>
