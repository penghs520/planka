import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由配置
 * 统一使用 AppLayout：工作区侧栏仅快捷入口；管理菜单仅在 /admin 下显示，经头像菜单进入
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
  // 统一主布局
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        redirect: '/workspace',
      },
      {
        path: 'workspace/issues',
        name: 'WorkspaceIssues',
        component: () => import('@/views/workspace/WorkspaceIssuesView.vue'),
        meta: {
          titleKey: 'sidebar.issues',
        },
      },
      {
        path: 'workspace/projects',
        name: 'WorkspaceProjects',
        component: () => import('@/views/workspace/WorkspaceProjectsView.vue'),
        meta: {
          titleKey: 'sidebar.projects',
        },
      },
      {
        path: 'workspace/teams',
        name: 'WorkspaceTeams',
        component: () => import('@/views/workspace/WorkspaceTeamsView.vue'),
        meta: {
          titleKey: 'sidebar.teams',
        },
      },
      {
        path: 'workspace/members',
        name: 'WorkspaceMembers',
        component: () => import('@/views/workspace/WorkspaceMembersView.vue'),
        meta: {
          titleKey: 'sidebar.members',
        },
      },
      // 工作区（视图列表）
      {
        path: 'workspace',
        name: 'Workspace',
        component: () => import('@/views/workspace/WorkspacePage.vue'),
        meta: {
          titleKey: 'common.route.workspace',
        },
      },
      {
        path: 'structure/:structureId/node/:nodeId',
        component: () => import('@/views/structure-node/StructureNodeLayout.vue'),
        redirect: (to) => ({
          path: `/structure/${to.params.structureId}/node/${to.params.nodeId}/issues`,
        }),
        children: [
          {
            path: 'issues',
            name: 'StructureNodeIssues',
            component: () => import('@/views/structure-node/StructureNodeIssuesView.vue'),
            meta: {
              titleKey: 'sidebar.teamIssues',
            },
          },
          {
            path: 'projects',
            name: 'StructureNodeProjects',
            component: () => import('@/views/structure-node/StructureNodeProjectsView.vue'),
            meta: {
              titleKey: 'sidebar.teamProjects',
            },
          },
          {
            path: 'views',
            name: 'StructureNodeViews',
            component: () => import('@/views/structure-node/StructureNodeViewsView.vue'),
            meta: {
              titleKey: 'sidebar.teamViews',
            },
          },
        ],
      },
      {
        path: 'team/:teamId',
        redirect: (to) => ({ path: `/team/${to.params.teamId}/issues` }),
      },
      {
        path: 'team/:teamId/issues',
        name: 'TeamIssues',
        component: () => import('@/views/team/TeamIssuesView.vue'),
        meta: {
          titleKey: 'sidebar.teamIssues',
        },
      },
      {
        path: 'team/:teamId/projects',
        name: 'TeamProjects',
        component: () => import('@/views/team/TeamProjectsView.vue'),
        meta: {
          titleKey: 'sidebar.teamProjects',
        },
      },
      {
        path: 'project/:projectId',
        redirect: (to) => ({ path: `/project/${to.params.projectId}/issues` }),
      },
      {
        path: 'project/:projectId/issues',
        name: 'ProjectIssues',
        component: () => import('@/views/project/ProjectIssuesView.vue'),
        meta: {
          titleKey: 'sidebar.issues',
        },
      },
      // TODO: Phase 4 - Inbox
      // {
      //   path: 'inbox',
      //   name: 'Inbox',
      //   component: () => import('@/views/inbox/InboxPage.vue'),
      //   meta: { titleKey: 'sidebar.inbox' },
      // },
      // 侧栏与前台导航个性化（可扩展更多项）
      {
        path: 'settings/sidebar',
        name: 'SidebarSettings',
        component: () => import('@/views/sidebar/SidebarSettingsView.vue'),
        meta: {
          titleKey: 'common.route.sidebarSettings',
        },
      },
      // 个人设置
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: {
          titleKey: 'common.route.profile',
        },
      },
      // 修改密码
      {
        path: 'change-password',
        name: 'UserChangePassword',
        component: () => import('@/views/user/ChangePasswordView.vue'),
        meta: {
          titleKey: 'common.route.changePassword',
        },
      },
      // ========================================
      // 管理后台（统一到同一布局下）
      // ========================================
      {
        path: 'admin',
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
          // 实体类型管理
          {
            path: 'card-type',
            name: 'CardTypeCard',
            component: () => import('@/views/schema-definition/card-type/CardView.vue'),
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
            },
          },
          {
            path: 'card-detail-template/:id/edit',
            name: 'CardDetailTemplateEdit',
            component: () => import('@/views/schema-definition/card-detail-template/TemplateEditor.vue'),
            meta: {
              titleKey: 'common.route.editDetailTemplate',
              activeMenu: 'card-detail-template',
            },
          },
          // 个人设置（管理后台入口）
          {
            path: 'profile',
            name: 'Profile',
            component: () => import('@/views/user/ProfileView.vue'),
            meta: {
              titleKey: 'common.route.profile',
            },
          },
          // 修改密码（管理后台入口）
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
          // 通知插件管理
          {
            path: 'notification-plugins',
            name: 'NotificationPlugins',
            component: () => import('@/views/admin/notification-plugins/index.vue'),
            meta: {
              titleKey: 'admin.plugin.title',
              activeMenu: 'notification-plugins',
            },
          },
          // 渠道配置
          {
            path: 'notification-channels/:channelId/config',
            name: 'ChannelConfig',
            component: () => import('@/views/admin/notification-channels/config.vue'),
            meta: {
              titleKey: 'admin.channel.config',
              activeMenu: 'notification-plugins',
            },
          },
        ],
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
