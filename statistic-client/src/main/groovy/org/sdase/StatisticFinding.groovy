package org.sdase

import groovy.time.TimeDuration
import groovy.util.logging.Slf4j
import io.securecodebox.persistence.defectdojo.models.Finding

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Slf4j
class StatisticFinding extends Finding {
    TimeDuration duration;
    String environment;
    String team;
    StatisticFinding(Finding finding, List<String> tags) {
        // copy fields
        for (Method getMethod : finding.getClass().getMethods()) {
            if (getMethod.getName().startsWith("get")) {
                try {
                    Method setMethod = this.getClass().getMethod(getMethod.getName().replace("get", "set"), getMethod.getReturnType());
                    setMethod.invoke(this, getMethod.invoke(finding, (Object[]) null));

                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {
                    //not found set
                }
            }
        }
        def today = new Date()
        def mitigatedDateOrToday = today;
        if (!finding.active) {
            if(finding.getMitigatedAt() != null) {
                mitigatedDateOrToday = finding.getMitigatedAt().toDate()
            }else if (finding.getRiskAccepted()) {
                mitigatedDateOrToday = finding.getCreatedAt().toDate()
                if(acceptedRisks.size() > 0 && acceptedRisks.get(0)) {
                    mitigatedDateOrToday = acceptedRisks.get(0).getCreatedAt().toDate()
                } else {
                    log.error "ERROR: finding ${finding.id} has a risk, but no object for it"
                }

            } else if (finding.verified) { //suppressed findings
                log.info "TODO suppressed for finding ${finding.id}; assuming creation date"
                mitigatedDateOrToday = finding.getCreatedAt().toDate()
            } else {
                log.debug "Inactive finding ${finding.id} (not mitigated)"
                mitigatedDateOrToday = finding.getCreatedAt().toDate()
            }
        }
        this.duration = groovy.time.TimeCategory.minus(
                mitigatedDateOrToday,
                finding.getCreatedAt().toDate()
        );
        this.team = findTagWithPrefix("team", tags)
        this.environment = findTagWithPrefix("cluster", tags)
    }

    static findTagWithPrefix(String prefix, List<String> tags) {
        for (tag in tags) {
            if (tag.startsWith("${prefix}/")) {
                return tag
            }
        }
        return null;
    }

    static Collection<StatisticFinding> findByEnvTeamSev(LinkedList<StatisticFinding> baseCollection, String environment, String team, String severity) {
        LinkedList<StatisticFinding> filteredFindings = new LinkedList<StatisticFinding>()
        log.info "env ${environment}; team ${team}, severity ${severity}"
        for(StatisticFinding statisticFinding in baseCollection) {
            if(statisticFinding != null && statisticFinding.getEnvironment() != null && statisticFinding.getTeam() != null && statisticFinding.getSeverity() != null) {
                if(statisticFinding.getEnvironment().matches(environment) && statisticFinding.getTeam().matches(team) && statisticFinding.getSeverity().name().matches(severity)) {
                    filteredFindings.push(statisticFinding)
                }
            }else {
                log.error "Error: statisticFinding ${statisticFinding.getId()} doesn't have all requred fields set"
            }
        }
        return filteredFindings
    }
}