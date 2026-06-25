package com.example.workflow.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import com.example.workflow.client.DossierClient;
import com.example.workflow.client.DossierDTO;
import com.example.workflow.client.DocumentClient;
import com.example.workflow.client.DocumentRequest;
import com.example.workflow.client.NotificationClient;
import com.example.workflow.client.NotificationRequest;
import com.example.workflow.client.PaiementClient;
import com.example.workflow.client.PaiementRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowIntegrationService {

    private static final String DEFAULT_DESTINATAIRE = "agent@localhost";

    private final DossierClient dossierClient;
    private final PaiementClient paiementClient;
    private final NotificationClient notificationClient;
    private final DocumentClient documentClient;

    public void updateDossierStatus(DelegateExecution execution, String statut) {
        Long dossierId = getDossierId(execution);
        dossierClient.updateStatut(dossierId, statut);
        execution.setVariable("statut", statut);
    }

    public void createDocumentIfPresent(DelegateExecution execution) {
        Long dossierId = getDossierId(execution);

        String nomFichier = getStringVariable(execution, "documentNomFichier");
        if (nomFichier == null || nomFichier.isBlank()) {
            return;
        }

        DocumentRequest req = new DocumentRequest();
        req.setDossierId(dossierId);
        req.setNomFichier(nomFichier);
        req.setType(getStringVariable(execution, "documentType"));
        req.setUrlStorage(getStringVariable(execution, "documentUrlStorage"));

        documentClient.create(req);
    }

    public void createPaiement(DelegateExecution execution) {
        Long dossierId = getDossierId(execution);
        Double montant = getDoubleVariable(execution, "montant", 100.0d);

        PaiementRequest request = new PaiementRequest();
        request.setDossierId(dossierId);
        request.setMontant(montant);
        request.setModePaiement("VIREMENT");
        request.setStatut("VALIDE");
        request.setReference("WF-" + dossierId + "-" + UUID.randomUUID().toString().substring(0, 8));
        request.setDatePaiement(LocalDateTime.now());

        paiementClient.createPaiement(request);
    }

    public void notifyServiceAssignment(DelegateExecution execution, String serviceGroup) {
        Long dossierId = getDossierId(execution);
        String targetGroup = (serviceGroup != null && !serviceGroup.isBlank()) ? serviceGroup : "DG";
        DossierDTO dossier = findDossier(dossierId);

        NotificationRequest request = new NotificationRequest();
        request.setDestinataire(targetGroup);
        request.setSujet("Nouveau dossier recu");
        request.setMessage(buildAssignmentMessage(dossier, dossierId));
        request.setType("IN_APP");
        request.setDossierId(dossierId);

        try {
            notificationClient.create(request);
        } catch (Exception e) {
            log.warn("Notification d'affectation non envoyee pour dossier {} vers {}: {}",
                    dossierId, targetGroup, e.getMessage());
        }
    }

    /** Notifie le DG qu'un dossier est prêt pour validation hiérarchique. */
    public void notifyDgForValidation(DelegateExecution execution) {
        Long dossierId = getDossierId(execution);
        DossierDTO dossier = findDossier(dossierId);

        NotificationRequest request = new NotificationRequest();
        request.setDestinataire("DG");
        request.setSujet("Dossier a valider");
        request.setMessage(buildDgValidationMessage(dossier, dossierId));
        request.setType("IN_APP");
        request.setDossierId(dossierId);

        try {
            notificationClient.create(request);
        } catch (Exception e) {
            log.warn("Notification DG non envoyee pour dossier {}: {}", dossierId, e.getMessage());
        }
    }

    /** Notifie le service qu'un complément est demandé par le DG. */
    public void notifyComplementRequested(DelegateExecution execution) {
        Long dossierId = getDossierId(execution);
        Object groupRaw = execution.getVariable("serviceCibleGroup");
        String targetGroup = (groupRaw instanceof String s && !s.isBlank()) ? s : "DG";
        if ("DG".equalsIgnoreCase(targetGroup)) {
            return;
        }

        String commentaire = getStringVariable(execution, "commentaire");
        DossierDTO dossier = findDossier(dossierId);

        NotificationRequest request = new NotificationRequest();
        request.setDestinataire(targetGroup);
        request.setSujet("Complement demande par le DG");
        String ref = dossier != null && dossier.getNumero() != null ? dossier.getNumero() : ("ID " + dossierId);
        String msg = "Le DG demande un complement pour le dossier " + ref + ".";
        if (commentaire != null && !commentaire.isBlank()) {
            msg += " Motif : " + commentaire;
        }
        request.setMessage(msg);
        request.setType("IN_APP");
        request.setDossierId(dossierId);

        try {
            notificationClient.create(request);
        } catch (Exception e) {
            log.warn("Notification complement non envoyee pour dossier {} vers {}: {}",
                    dossierId, targetGroup, e.getMessage());
        }
    }

    public void notifyResult(DelegateExecution execution, boolean validated) {
        Long dossierId = getDossierId(execution);
        String destinataire = resolveDestinataire(execution, dossierId);
        String sujet = validated
                ? "Dossier valide et archive"
                : "Dossier rejete";
        String message = validated
                ? "Le dossier " + dossierId + " a termine le workflow (valide, paye, archive)."
                : "Le dossier " + dossierId + " a ete rejete pendant la validation.";

        NotificationRequest request = new NotificationRequest();
        request.setDestinataire(destinataire);
        request.setSujet(sujet);
        request.setMessage(message);
        request.setType("IN_APP");
        request.setDossierId(dossierId);

        notificationClient.create(request);
    }

    private Long getDossierId(DelegateExecution execution) {
        Object raw = execution.getVariable("dossierId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Variable dossierId absente ou invalide dans le workflow");
    }

    private Double getDoubleVariable(DelegateExecution execution, String key, Double defaultValue) {
        Object raw = execution.getVariable(key);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    private String getStringVariable(DelegateExecution execution, String key) {
        Object raw = execution.getVariable(key);
        if (raw instanceof String value && !value.isBlank()) {
            return value;
        }
        return null;
    }

    private DossierDTO findDossier(Long dossierId) {
        try {
            return dossierClient.getDossierById(dossierId);
        } catch (Exception e) {
            log.warn("Impossible de recuperer le dossier {}: {}", dossierId, e.getMessage());
            return null;
        }
    }

    private String buildAssignmentMessage(DossierDTO dossier, Long dossierId) {
        String reference = "ID " + dossierId;
        if (dossier != null && dossier.getNumero() != null && !dossier.getNumero().isBlank()) {
            reference = dossier.getNumero();
        }

        String titre = dossier != null ? dossier.getTitre() : null;
        if (titre != null && !titre.isBlank()) {
            return "Le Bureau d'Ordre vous a envoye le dossier " + reference + " (" + titre
                    + "). Merci de proceder au traitement.";
        }

        return "Le Bureau d'Ordre vous a envoye le dossier " + reference
                + ". Merci de proceder au traitement.";
    }

    private String buildDgValidationMessage(DossierDTO dossier, Long dossierId) {
        String reference = "ID " + dossierId;
        if (dossier != null && dossier.getNumero() != null && !dossier.getNumero().isBlank()) {
            reference = dossier.getNumero();
        }
        String titre = dossier != null ? dossier.getTitre() : null;
        if (titre != null && !titre.isBlank()) {
            return "Le dossier " + reference + " (" + titre
                    + ") a ete traite par le service. Validation hierarchique requise.";
        }
        return "Le dossier " + reference + " a ete traite par le service. Validation hierarchique requise.";
    }

    private String resolveDestinataire(DelegateExecution execution, Long dossierId) {
        Object rawDestinataire = execution.getVariable("destinataire");
        if (rawDestinataire instanceof String value && !value.isBlank()) {
            return value;
        }

        DossierDTO dossier = findDossier(dossierId);
        if (dossier != null && dossier.getUserId() != null) {
            return "user-" + dossier.getUserId() + "@localhost";
        }

        return DEFAULT_DESTINATAIRE;
    }
}
