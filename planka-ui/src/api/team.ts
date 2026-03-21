import { cardApi } from '@/api/card'
import {
  issueCardTypeId,
  projectCardTypeId,
  teamCardTypeId,
  teamMemberLinkTypeId,
  teamProjectLinkTypeId,
  projectIssueLinkTypeId,
} from '@/constants/systemSchemaIds'
import type { CardDTO } from '@/types/card'
import { createCardPageQuery, conditionLinkIn } from '@/utils/card-query'
import { buildLinkFieldId } from '@/utils/link-field-utils'

/**
 * 当前成员所在的团队（Team 卡）
 */
export async function fetchMyTeams(
  orgId: string,
  operatorMemberCardId: string,
): Promise<CardDTO[]> {
  const linkFieldId = buildLinkFieldId(teamMemberLinkTypeId(orgId), 'SOURCE')
  const req = createCardPageQuery(orgId, operatorMemberCardId, [teamCardTypeId(orgId)])
  req.condition = conditionLinkIn(linkFieldId, [operatorMemberCardId])
  const page = await cardApi.pageQuery(req)
  return page.content
}

/**
 * 某团队下的 Project 卡
 */
export async function fetchProjectsForTeam(
  orgId: string,
  operatorMemberCardId: string,
  teamCardId: string,
): Promise<CardDTO[]> {
  const linkFieldId = buildLinkFieldId(teamProjectLinkTypeId(orgId), 'TARGET')
  const req = createCardPageQuery(orgId, operatorMemberCardId, [projectCardTypeId(orgId)])
  req.condition = conditionLinkIn(linkFieldId, [teamCardId])
  const page = await cardApi.pageQuery(req)
  return page.content
}

/**
 * 组织内全部 Project
 */
export async function fetchAllProjectsInOrg(
  orgId: string,
  operatorMemberCardId: string,
): Promise<CardDTO[]> {
  const req = createCardPageQuery(orgId, operatorMemberCardId, [projectCardTypeId(orgId)])
  const page = await cardApi.pageQuery(req)
  return page.content
}

/**
 * 组织内全部 Issue
 */
export async function fetchAllIssuesInOrg(
  orgId: string,
  operatorMemberCardId: string,
): Promise<CardDTO[]> {
  const req = createCardPageQuery(orgId, operatorMemberCardId, [issueCardTypeId(orgId)])
  const page = await cardApi.pageQuery(req)
  return page.content
}

/**
 * 多个项目下的 Issue（OR 语义：所属项目 ∈ projectCardIds）
 */
export async function fetchIssuesForProjects(
  orgId: string,
  operatorMemberCardId: string,
  projectCardIds: string[],
): Promise<CardDTO[]> {
  if (projectCardIds.length === 0) {
    return []
  }
  const linkFieldId = buildLinkFieldId(projectIssueLinkTypeId(orgId), 'TARGET')
  const req = createCardPageQuery(orgId, operatorMemberCardId, [issueCardTypeId(orgId)])
  req.condition = conditionLinkIn(linkFieldId, projectCardIds)
  const page = await cardApi.pageQuery(req)
  return page.content
}

/**
 * 单个项目下的 Issue
 */
export async function fetchIssuesForProject(
  orgId: string,
  operatorMemberCardId: string,
  projectCardId: string,
): Promise<CardDTO[]> {
  return fetchIssuesForProjects(orgId, operatorMemberCardId, [projectCardId])
}

/**
 * 组织内全部 Team（目录）
 */
export async function fetchAllTeamsInOrg(
  orgId: string,
  operatorMemberCardId: string,
): Promise<CardDTO[]> {
  const req = createCardPageQuery(orgId, operatorMemberCardId, [teamCardTypeId(orgId)])
  const page = await cardApi.pageQuery(req)
  return page.content
}
