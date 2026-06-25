package com.example.workflow.delegate;

import com.example.workflow.service.WorkflowIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Décision du DG (responsable hiérarchique) : approbation, rejet ou demande de complément.
 */
@Slf4j
@Component("validationDelegate")
@RequiredArgsConstructor
public class ValidationDelegate implements JavaDelegate {

    private final WorkflowIntegrationService integrationService;

    @Override
    public void execute(DelegateExecution execution) {
        String decision = normalizeDecision(execution.getVariable("validationDecision"));
        execution.setVariable("validationDecision", decision);

        switch (decision) {
            case "APPROUVE" -> {
                execution.setVariable("isValidated", true);
                integrationService.updateDossierStatus(execution, "VALIDE");
                log.info("Dossier {} approuvé par le DG", execution.getVariable("dossierId"));
            }
            case "REJETE" -> {
                execution.setVariable("isValidated", false);
                log.info("Dossier {} rejeté par le DG", execution.getVariable("dossierId"));
            }
            case "COMPLEMENT" -> {
                execution.setVariable("isValidated", false);
                integrationService.updateDossierStatus(execution, "EN_COURS");
                integrationService.notifyComplementRequested(execution);
                log.info("Complément demandé par le DG pour dossier {}", execution.getVariable("dossierId"));
            }
            default -> throw new IllegalArgumentException("Décision DG invalide: " + decision);
        }
    }

    private String normalizeDecision(Object raw) {
        if (raw == null) return "REJETE";
        String value = String.valueOf(raw).trim().toUpperCase();
        return switch (value) {
            case "APPROUVE", "VALIDE", "APPROVED", "TRUE" -> "APPROUVE";
            case "REJETE", "REJET", "REJECTED", "FALSE" -> "REJETE";
            case "COMPLEMENT", "COMPLEMENTAIRE", "COMPLEMENT_INFO" -> "COMPLEMENT";
            default -> value;
        };
    }
}
