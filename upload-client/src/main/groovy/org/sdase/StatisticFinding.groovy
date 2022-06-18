package org.sdase

import groovy.time.TimeDuration
import io.securecodebox.persistence.defectdojo.models.Finding

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

public class StatisticFinding extends Finding {
    TimeDuration duration;
    String environment;
    String team;
    public StatisticFinding(Finding finding, List<String> tags) {
        // copy fields
        for (Method getMethod : finding.getClass().getMethods()) {
            if (getMethod.getName().startsWith("get")) {
                try {
                    Method setMethod = this.getClass().getMethod(getMethod.getName().replace("get", "set"), getMethod.getReturnType());
                    setMethod.invoke(this, getMethod.invoke(finding, (Object[]) null));

                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
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
                if(acceptedRisks.get(0)) {
                    mitigatedDateOrToday = acceptedRisks.get(0).getCreatedAt().toDate()
                } else {
                    println "ERROR: finding ${finding.id} has a risk, but no object for it"
                }

            } else if (finding.verified) { //suppressed findings
                println "TODO suppressed for finding ${finding.id}; assuming creation date"
                mitigatedDateOrToday = finding.getCreatedAt().toDate()
            } else {
                println "Unknown state for finding ${finding.id} in product ${product.id}"
            }
        }
        this.duration = groovy.time.TimeCategory.minus(
                mitigatedDateOrToday,
                finding.getCreatedAt().toDate()
        );
        this.team = getTag("team", tags)
        this.environment = getTag("cluster", tags)
    }

    public static getTag(String expectedTag, List<String> tags) {
        for (tag in tags) {
            if (tag.startsWith("${expectedTag}/")) {
                return tag
            }
        }
        return null;
    }

    public static LinkedList<StatisticFinding> findEnvironment(LinkedList<StatisticFinding> baseCollection, String env, String team, String severity) {
        LinkedList<StatisticFinding> filteredFindings = new LinkedList<StatisticFinding>()
        println "env ${env}; team ${team}, severity ${severity}"
        for(StatisticFinding statisticFinding in baseCollection) {
            if(statisticFinding != null && statisticFinding.getEnvironment() != null && statisticFinding.getTeam() != null && statisticFinding.getSeverity() != null) {
                if(statisticFinding.getEnvironment().matches(env) && statisticFinding.getTeam().matches(team) && statisticFinding.getSeverity().name().matches(severity)) {
                    filteredFindings.push(statisticFinding)
                }
            }else {
                print "Error: statisticFinding ${statisticFinding.getId()} doesn't have all requred fields set"
            }
        }
        return filteredFindings
    }
}