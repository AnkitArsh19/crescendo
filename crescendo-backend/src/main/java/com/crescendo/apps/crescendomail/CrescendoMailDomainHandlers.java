package com.crescendo.apps.crescendomail;

import com.crescendo.emailservice.domain.DomainCommandService;
import com.crescendo.emailservice.domain.DomainDto;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.blank;
import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.resolveUserId;

/**
 * Domain action handler for crescendomail.
 *
 * <p>Action key:
 * <ul>
 *   <li>{@code crescendomail:verify-domain} — trigger an on-demand DNS recheck for a pending domain
 * </ul>
 *
 * <p>Useful in an "alert me if my domain breaks" workflow: this action forces a recheck rather
 * than waiting for the next scheduled poll, so status changes surface faster.
 */
public class CrescendoMailDomainHandlers {

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "verify-domain")
    public static class VerifyDomainHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(VerifyDomainHandler.class);

        private final DomainCommandService domainCommandService;

        public VerifyDomainHandler(DomainCommandService domainCommandService) {
            this.domainCommandService = domainCommandService;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String domainIdStr = ctx.getString("domainId");
            if (blank(domainIdStr)) return ActionResult.failure("crescendomail:verify-domain requires 'domainId'");

            UUID domainId;
            try {
                domainId = UUID.fromString(domainIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid domainId format");
            }

            UUID userId = resolveUserId(ctx);
            DomainDto.DomainResponse result = domainCommandService.verifyDomain(userId, domainId);

            log.info("[crescendomail:verify-domain] Verified domain {} → status={}", domainId, result.status());
            return ActionResult.success(Map.of(
                    "domainId", result.id().toString(),
                    "domain",   result.domainName(),
                    "status",   result.status()
            ));
        }
    }
}
