<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import SmartAgentWidget from '@/components/SmartAgentWidget.vue';
</script>

<template>
  <el-config-provider :locale="zhCn">
    <router-view></router-view>
    <SmartAgentWidget />
  </el-config-provider>
</template>
