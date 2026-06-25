package com.example.workflow.delegate;

import com.example.workflow.client.DossierClient;
import com.example.workflow.client.DossierDTO;
import com.example.workflow.service.WorkflowIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("enregistrementDelegate")
@RequiredArgsConstructor
public class EnregistrementDelegate implements JavaDelegate {

    private final WorkflowIntegrationService integrationService;
    private final DossierClient dossierClient;

    @Override
    public void execute(DelegateExecution execution) {
        // 1. Mettre à jour le statut du dossier
        integrationService.updateDossierStatus(execution, "ENREGISTRE");

        // 2. Calculer le groupe candidat pour UserTask_Decision selon le service cible
        String serviceCibleGroup = resolveServiceGroup(execution);
        execution.setVariable("serviceCibleGroup", serviceCibleGroup);
        if ("DG".equalsIgnoreCase(serviceCibleGroup)) {
            integrationService.notifyDgForValidation(execution);
        } else {
            integrationService.notifyServiceAssignment(execution, serviceCibleGroup);
        }
        log.info("Dossier {} -> serviceCibleGroup = {}", execution.getVariable("dossierId"), serviceCibleGroup);
    }

    /**
     * Résout le groupe Camunda (DG, RH, TECHNIQUE, JURIDIQUE) depuis le champ serviceCible du dossier.
     */
    private String resolveServiceGroup(DelegateExecution execution) {
        // D'abord, essayer depuis la variable de processus (passée au démarrage si disponible)
        Object raw = execution.getVariable("serviceCible");
        String serviceCible = (raw instanceof String s && !s.isBlank()) ? s : null;

        // Sinon, charger depuis le dossier-service
        if (serviceCible == null) {
            try {
                Object rawId = execution.getVariable("dossierId");
                if (rawId instanceof Number n) {
                    DossierDTO dossier = dossierClient.getDossierById(n.longValue());
                    if (dossier != null) {
                        serviceCible = dossier.getServiceCible();
                    }
                }
            } catch (Exception e) {
                log.warn("Impossible de récupérer serviceCible depuis dossier-service: {}", e.getMessage());
            }
        }

        return mapToGroup(serviceCible);
    }

    private String mapToGroup(String serviceCible) {
        if (serviceCible == null) return "DG";
        String s = serviceCible.trim().toLowerCase();
        if (s.contains("rh") || s.contains("ressources")) return "RH";
        if (s.contains("technique")) return "TECHNIQUE";
        if (s.contains("juridique")) return "JURIDIQUE";
        if (s.contains("financier") || s.contains("finance")) return "FINANCIER";
        if (s.contains("direction") || s.contains("générale") || s.contains("generale") || s.contains("dg")) return "DG";
        // fallback
        return "DG";
    }
}
