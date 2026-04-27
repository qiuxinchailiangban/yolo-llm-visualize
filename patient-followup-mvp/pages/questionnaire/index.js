const { request } = require("../../utils/request");

const SUBMIT_MODE_INTAKE_NEW_PATIENT = "INTAKE_NEW_PATIENT";
const SUBMIT_MODE_FOLLOW_UP_SHARED = "FOLLOW_UP_SHARED";
const SUBMIT_MODE_FOLLOW_UP_TASK = "FOLLOW_UP_TASK";

Page({
  data: {
    mode: "intake",
    submitMode: SUBMIT_MODE_INTAKE_NEW_PATIENT,
    loading: true,
    taskNo: "",
    token: "",
    patientId: "",
    patientName: "",
    title: "",
    stageName: "",
    items: [],
    patientForm: {
      name: "",
      gender: "",
      phone: "",
      birthDate: "",
      surgeryDate: "",
      diagnosis: "",
    },
    followUpForm: {
      name: "",
      phone: "",
    },
    answers: {},
  },

  onLoad(options) {
    const scene = options.scene ? decodeURIComponent(options.scene) : "";
    const token = options.token || scene || "";
    const taskNo = options.taskNo || "";
    if (token) {
      this.setData({ token });
      this.loadFromQrToken(token);
    } else if (taskNo) {
      this.setData({ mode: "task", submitMode: SUBMIT_MODE_FOLLOW_UP_TASK, taskNo });
      this.loadTask(taskNo);
    } else {
      this.loadIntakeTemplate();
    }
  },

  async loadFromQrToken(token) {
    try {
      const data = await request({ url: `/api/public/qrcode/resolve?token=${encodeURIComponent(token)}` });
      const schema = JSON.parse(data.template?.schemaJson || '{"items":[]}');
      const submitMode = data.submitMode || (data.collectPatientInfo ? SUBMIT_MODE_INTAKE_NEW_PATIENT : SUBMIT_MODE_FOLLOW_UP_TASK);

      let mode;
      if (submitMode === SUBMIT_MODE_INTAKE_NEW_PATIENT) {
        mode = "intake";
      } else if (submitMode === SUBMIT_MODE_FOLLOW_UP_SHARED) {
        mode = "follow_up_shared";
      } else {
        mode = "task";
      }

      this.setData({
        mode,
        submitMode,
        taskNo: data.taskNo || "",
        patientId: data.patientId || "",
        patientName: data.patientName || "",
        stageName: data.stageName || "",
        title: data.template?.templateName || "患者问卷",
        items: schema.items || [],
        loading: false,
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
      this.setData({ loading: false });
    }
  },

  async loadIntakeTemplate() {
    try {
      const data = await request({ url: "/api/public/intake-template" });
      const schema = JSON.parse(data.schemaJson || '{"items":[]}');
      this.setData({
        title: data.templateName,
        items: schema.items || [],
        loading: false,
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
      this.setData({ loading: false });
    }
  },

  async loadTask(taskNo) {
    try {
      const data = await request({ url: `/api/public/tasks/${taskNo}` });
      const schema = JSON.parse(data.template.schemaJson || '{"items":[]}');
      this.setData({
        title: data.template.templateName,
        patientId: data.patientId,
        patientName: data.patientName,
        stageName: data.stageName,
        items: schema.items || [],
        loading: false,
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
      this.setData({ loading: false });
    }
  },

  onPatientInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`patientForm.${field}`]: e.detail.value,
    });
  },

  onFollowUpInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`followUpForm.${field}`]: e.detail.value,
    });
  },

  onAnswerInput(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({
      [`answers.${key}`]: e.detail.value,
    });
  },

  async submit() {
    try {
      if (this.data.mode === "task") {
        await request({
          url: `/api/public/tasks/${this.data.taskNo}/submit`,
          method: "POST",
          data: { answers: this.data.answers },
        });
      } else if (this.data.mode === "follow_up_shared") {
        const name = (this.data.followUpForm.name || "").trim();
        const phone = (this.data.followUpForm.phone || "").trim();
        if (!name || !phone) {
          wx.showToast({ title: "请填写姓名和手机号", icon: "none" });
          return;
        }
        await request({
          url: "/api/public/follow-up-submissions",
          method: "POST",
          data: {
            token: this.data.token,
            name,
            phone,
            answers: this.data.answers,
          },
        });
      } else {
        await request({
          url: "/api/public/intake-submissions",
          method: "POST",
          data: {
            ...this.data.patientForm,
            answers: this.data.answers,
          },
        });
      }
      wx.showToast({ title: "提交成功", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },
});
