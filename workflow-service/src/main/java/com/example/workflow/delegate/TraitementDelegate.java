package com.example.workflow.delegate;

import com.example.workflow.service.WorkflowIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Après traitement par le service (RH, Juridique, Technique, Financier) :
 * passe le dossier en EN_TRAITEMENT et notifie le DG pour validation hiérarchique.
 */
@Slf4j
@Component("traitementDelegate")
@RequiredArgsConstructor
public class TraitementDelegate implements JavaDelegate {

    private final WorkflowIntegrationService integrationService;

    @Override
    public void execute(DelegateExecution execution) {
        integrationService.updateDossierStatus(execution, "EN_TRAITEMENT");
        integrationService.notifyDgForValidation(execution);
        log.info("Traitement terminé pour dossier {} — en attente validation DG",
                execution.getVariable("dossierId"));
    }
}
