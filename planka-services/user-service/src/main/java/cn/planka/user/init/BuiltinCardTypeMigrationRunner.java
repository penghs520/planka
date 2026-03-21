package cn.planka.user.init;

import cn.planka.user.repository.OrganizationRepository;
import cn.planka.user.service.BuiltinCardTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 存量组织补建内置 Team / Project / Issue 及后续新增关联（如负责人，幂等）。
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class BuiltinCardTypeMigrationRunner implements ApplicationRunner {

    private final OrganizationRepository organizationRepository;
    private final BuiltinCardTypeService builtinCardTypeService;

    @Override
    public void run(ApplicationArguments args) {
        var orgs = organizationRepository.findAllActive();
        if (orgs.isEmpty()) {
            return;
        }
        log.info("Builtin card type migration: checking {} organizations", orgs.size());
        for (var org : orgs) {
            String orgId = org.getId();
            try {
                boolean hasTeam = builtinCardTypeService.hasBuiltinTeamType(orgId);
                boolean hasTeamLead = builtinCardTypeService.hasTeamLeadLinkType(orgId);
                if (hasTeam && hasTeamLead) {
                    continue;
                }
                builtinCardTypeService.initBuiltinTypes(orgId);
            } catch (Exception e) {
                log.error("Builtin card type migration failed for org {}: {}", orgId, e.getMessage(), e);
            }
        }
    }
}
