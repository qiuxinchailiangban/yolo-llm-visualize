import { createRouter, createWebHistory } from "vue-router";
import DashboardView from "../views/DashboardView.vue";
import AutomationJobListView from "../views/AutomationJobListView.vue";
import LoginView from "../views/LoginView.vue";
import PatientListView from "../views/PatientListView.vue";
import PatientProcessDashboardView from "../views/PatientProcessDashboardView.vue";
import PatientProcessExceptionCenterView from "../views/PatientProcessExceptionCenterView.vue";
import PatientProcessTemplateView from "../views/PatientProcessTemplateView.vue";
import MessageTriggerRuleListView from "../views/MessageTriggerRuleListView.vue";
import StageListView from "../views/StageListView.vue";
import TaskListView from "../views/TaskListView.vue";
import TemplateListView from "../views/TemplateListView.vue";
import WechatGroupLeadListView from "../views/WechatGroupLeadListView.vue";
import { isLoggedIn } from "../utils/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/dashboard" },
    { path: "/login", component: LoginView, meta: { public: true } },
    { path: "/dashboard", component: DashboardView },
    { path: "/automation-jobs", component: AutomationJobListView },
    { path: "/patients", component: PatientListView },
    { path: "/patient-processes", component: PatientProcessDashboardView },
    { path: "/patient-process-exceptions", component: PatientProcessExceptionCenterView },
    { path: "/patient-process-templates", component: PatientProcessTemplateView },
    { path: "/message-trigger-rules", component: MessageTriggerRuleListView },
    { path: "/wechat-group-leads", component: WechatGroupLeadListView },
    { path: "/templates", component: TemplateListView },
    { path: "/stages", component: StageListView },
    { path: "/tasks", component: TaskListView },
  ],
});

router.beforeEach((to) => {
  if (to.meta.public) {
    return true;
  }
  if (!isLoggedIn()) {
    return "/login";
  }
  return true;
});

export default router;
