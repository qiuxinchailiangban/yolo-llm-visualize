<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">患者随访管理系统</div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/dashboard">首页待办</el-menu-item>
        <el-menu-item index="/automation-jobs">自动化任务</el-menu-item>
        <el-menu-item index="/patients">患者管理</el-menu-item>
        <el-menu-item index="/patient-processes">患者流程</el-menu-item>
        <el-menu-item index="/patient-process-exceptions">流程异常中心</el-menu-item>
        <el-menu-item index="/wechat-group-leads">微信群线索</el-menu-item>
        <el-menu-item index="/message-trigger-rules">任务中心</el-menu-item>
        <el-menu-item index="/patient-process-templates">流程模板</el-menu-item>
        <el-menu-item index="/tasks">问卷任务</el-menu-item>
        <el-menu-item index="/stages">随访阶段</el-menu-item>
        <el-menu-item index="/templates">问卷模板</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>医院患者随访问卷管理系统 V2</span>
        <div class="header-right">
          <span>{{ userName }}</span>
          <el-button link type="primary" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { logout } from "./api/auth";
import { clearAuth, getUser } from "./utils/auth";

const route = useRoute();
const router = useRouter();
const activeMenu = computed(() => route.path);
const isLoginPage = computed(() => route.path === "/login");
const userName = ref("");

watch(
  () => route.path,
  () => {
    userName.value = getUser()?.displayName || "未登录";
  },
  { immediate: true }
);

async function handleLogout() {
  try {
    await logout();
  } finally {
    clearAuth();
    await router.push("/login");
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.sidebar {
  background: #001529;
  color: #fff;
}

.logo {
  padding: 20px 16px;
  font-size: 18px;
  font-weight: 600;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 14px;
  font-weight: 400;
}

.main {
  background: #f5f7fa;
}
</style>
