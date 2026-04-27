<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="title">后台登录</div>
      </template>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit" @click="submitLogin">登录</el-button>
      </el-form>
      <div class="tips">默认账号：admin / admin123456</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { reactive } from "vue";
import { useRouter } from "vue-router";
import { login } from "../api/auth";
import { saveAuth } from "../utils/auth";

const router = useRouter();
const form = reactive({
  username: "admin",
  password: "admin123456",
});

async function submitLogin() {
  try {
    const result = await login(form.username, form.password);
    saveAuth(result);
    ElMessage.success("登录成功");
    await router.push("/dashboard");
  } catch (error) {
    ElMessage.error("登录失败，请检查账号密码");
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1677ff, #6aa9ff);
}

.login-card {
  width: 420px;
}

.title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
}

.submit {
  width: 100%;
}

.tips {
  margin-top: 16px;
  color: #909399;
  text-align: center;
  font-size: 13px;
}
</style>
