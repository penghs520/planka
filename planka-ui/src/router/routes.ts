import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由配置
 * 注意：meta.titleKey 用于国际化，在路由守卫中解析为实际标题
 */
export const routes: RouteRecordRaw[] = [
  // 认证相关页面（不需要登录）
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: {
      titleKey: 'common.route.login',
      requiresAuth: false,
    },
  },
  {
    path: '/activate',
    name: 'Activate',
    component: () => import('@/views/auth/ActivateView.vue'),
    meta: {
      titleKey: 'common.route.activate',
      requiresAuth: false,
    },
  },
  // 组织选择页（需要登录，但不需要选择组织）
  {
    path: '/select-org',
    name: 'SelectOrg',
    component: () => import('@/views/auth/SelectOrgView.vue'),
    meta: {
      titleKey: 'common.route.selectOrg',
      requiresAuth: true,
      skipOrgCheck: true,
    },
  },
  // 用户侧工作区（独立布局）
  {
    path: '/',
    component: () => import('@/layouts/WorkspaceLayout.vue'),
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        redirect: '/workspace',
      },
      {
        path: 'workspace',
        name: 'Workspace',
        component: () => import('@/views/workspace/WorkspacePage.vue'),
        meta: {
          titleKey: 'common.route.workspace',
        },
      },
      // 个人设置（用户侧）
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: {
          titleKey: 'common.route.profile',
        },
      },
      // 修改密码（用户侧）
      {
        path: 'change-password',
        name: 'UserChangePassword',
        component: () => import('@/views/user/ChangePasswordView.vue'),
        meta: {
          titleKey: 'common.route.changePassword',
        },
      },
      // 考勤模块（用户侧）
      {
        path: 'attendance',
        redirect: '/attendance/clock',
        meta: {
          titleKey: 'attendance.title',
        },
        children: [
          {
            path: 'clock',
            name: 'AttendanceClock',
            component: () => import('@/views/attendance/ClockPage.vue'),
            meta: {
              titleKey: 'attendance.clock',
            },
          },
          {
            path: 'my-records',
            name: 'AttendanceMyRecords',
            component: () => import('@/views/attendance/MyRecordsPage.vue'),
            meta: {
              titleKey: 'attendance.myRecords',
            },
          },
          {
            path: 'applications',
            name: 'AttendanceApplications',
            component: () => import('@/views/attendance/ApplicationsPage.vue'),
            meta: {
              titleKey: 'attendance.applications',
            },
          },
          {
            path: 'approvals',
            name: 'AttendanceApprovals',
            component: () => import('@/views/attendance/ApprovalsPage.vue'),
            meta: {
              titleKey: 'attendance.approvals',
            },
          },
        ],
      },
    ],
  },
  // 管理后台（需要登录和选择组织）
  {
    path: '/admin',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        redirect: '/admin/card-type',
      },
      // 计算公式定义管理
      {
        path: 'formula-definition',
        name: 'FormulaDefinitionList',
        component: () => import('@/views/schema-definition/formula-definition/ListView.vue'),
        meta: {
          titleKey: 'admin.formulaDefinition.title',
          activeMenu: 'formula-definition',
        },
      },
      // 卡片类型管理
      {
        path: 'card-type',
        name: 'CardTypeList',
        component: () => import('@/views/schema-definition/card-type/ListView.vue'),
        meta: {
          titleKey: 'admin.cardType.title',
          activeMenu: 'card-type',
        },
      },
      // 关联类型管理
      {
        path: 'link-type',
        name: 'LinkType',
        redirect: '/admin/link-type/list',
        meta: {
          titleKey: 'admin.linkType.title',
          activeMenu: 'link-type',
        },
        children: [
          {
            path: 'list',
            name: 'LinkTypeList',
            component: () => import('@/views/schema-definition/link-type/ListView.vue'),
            meta: {
              titleKey: 'common.route.linkTypeList',
              activeMenu: 'link-type',
            },
          },
          {
            path: 'graph',
            name: 'LinkTypeGraph',
            component: () => import('@/views/schema-definition/link-type/ERDiagramView.vue'),
            meta: {
              titleKey: 'common.route.linkTypeGraph',
              activeMenu: 'link-type',
            },
          },
        ],
      },
      // 架构线定义
      {
        path: 'structure',
        name: 'Structure',
        component: () => import('@/views/schema-definition/structure/StructureGraphView.vue'),
        meta: {
          titleKey: 'admin.structure.title',
          activeMenu: 'structure',
        },
      },
      // 视图配置
      {
        path: 'view',
        name: 'ViewList',
        component: () => import('@/views/schema-definition/view/ListView.vue'),
        meta: {
          titleKey: 'admin.view.title',
          activeMenu: 'view',
        },
      },
      // 菜单配置
      {
        path: 'menu',
        name: 'MenuConfig',
        component: () => import('@/views/schema-definition/menu/MenuConfigView.vue'),
        meta: {
          titleKey: 'admin.menuConfig.title',
          activeMenu: 'menu',
        },
      },
      // 考勤配置
      {
        path: 'outsourcing-config',
        name: 'OutsourcingConfig',
        component: () => import('@/views/schema-definition/outsourcing-config/index.vue'),
        meta: {
          titleKey: 'outsourcingConfig.title',
          activeMenu: 'outsourcing-config',
        },
      },
      // 工时管理
      {
        path: 'workload-management',
        name: 'WorkloadManagement',
        component: () => import('@/views/admin/workload-management/index.vue'),
        meta: {
          titleKey: 'admin.workloadManagement.title',
          activeMenu: 'workload-management',
        },
      },
      // 详情页模板
      {
        path: 'card-detail-template',
        name: 'CardDetailTemplateList',
        component: () => import('@/views/schema-definition/card-detail-template/ListView.vue'),
        meta: {
          titleKey: 'common.route.detailTemplate',
          activeMenu: 'card-detail-template',
        },
      },
      {
        path: 'card-detail-template/create',
        name: 'CardDetailTemplateCreate',
        component: () => import('@/views/schema-definition/card-detail-template/TemplateEditor.vue'),
        meta: {
          titleKey: 'common.route.createDetailTemplate',
          activeMenu: 'card-detail-template',
          hideMenu: true,
        },
      },
      {
        path: 'card-detail-template/:id/edit',
        name: 'CardDetailTemplateEdit',
        component: () => import('@/views/schema-definition/card-detail-template/TemplateEditor.vue'),
        meta: {
          titleKey: 'common.route.editDetailTemplate',
          activeMenu: 'card-detail-template',
          hideMenu: true,
        },
      },
      // 个人设置
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: {
          titleKey: 'common.route.profile',
        },
      },
      // 修改密码
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/user/ChangePasswordView.vue'),
        meta: {
          titleKey: 'common.route.changePassword',
        },
      },
      // 成员管理
      {
        path: 'members',
        name: 'MemberManagement',
        component: () => import('@/views/org/MemberManagementView.vue'),
        meta: {
          titleKey: 'admin.members.title',
          activeMenu: 'members',
        },
      },
      // 组织设置
      {
        path: 'org-settings',
        name: 'OrgSettings',
        component: () => import('@/views/org/SettingsView.vue'),
        meta: {
          titleKey: 'admin.orgSettings.title',
          activeMenu: 'org-settings',
        },
      },
      // 审计日志
      {
        path: 'audit-log',
        name: 'AuditLog',
        component: () => import('@/views/audit-log/AuditLogView.vue'),
        meta: {
          titleKey: 'admin.auditLog.title',
          activeMenu: 'audit-log',
        },
      },
      // 通知设置
      {
        path: 'notification-settings',
        name: 'NotificationSettings',
        component: () => import('@/views/admin/notification-settings/index.vue'),
        meta: {
          titleKey: 'admin.notificationSettings.title',
          activeMenu: 'notification-settings',
        },
      },
    ],
  },
  // 独立卡片详情页
  {
    path: '/card/:cardId',
    name: 'CardDetail',
    component: () => import('@/views/workspace/components/card-detail/CardDetailPage.vue'),
    meta: {
      titleKey: 'common.route.cardDetail',
      requiresAuth: true,
    },
  },
  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/',
  },
]
