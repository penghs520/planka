/**
 * 与后端 {@link cn.planka.common.util.SystemSchemaIds} 一致的 ID 拼接（按当前组织）
 */
export function teamCardTypeId(orgId: string) {
  return `${orgId}:team`
}

export function projectCardTypeId(orgId: string) {
  return `${orgId}:project`
}

export function issueCardTypeId(orgId: string) {
  return `${orgId}:issue`
}

export function teamMemberLinkTypeId(orgId: string) {
  return `${orgId}:link:team-member`
}

export function teamProjectLinkTypeId(orgId: string) {
  return `${orgId}:link:team-project`
}

export function projectIssueLinkTypeId(orgId: string) {
  return `${orgId}:link:project-issue`
}

export function teamLeadLinkTypeId(orgId: string) {
  return `${orgId}:link:team-lead`
}

export function projectLeadLinkTypeId(orgId: string) {
  return `${orgId}:link:project-lead`
}
