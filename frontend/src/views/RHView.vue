<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useAppStore } from '../stores/appStore'

const store = useAppStore()

// Page navigation synchronized with the Pinia store
const page = computed({
  get: () => {
    if (store.activeView === 'rh-traiter') return 'traiter'
    if (store.activeView === 'rh-nouveau') return 'nouveau'
    return 'dashboard'
  },
  set: (val: string) => {
    if (val === 'traiter') store.activeView = 'rh-traiter'
    else if (val === 'nouveau') store.activeView = 'rh-nouveau'
    else store.activeView = 'rh-dashboard'
  }
})

// Local state
const rhFiles = ref<string[]>(['CV_Candidat.pdf'])
const selectedDossier = ref<any>(null)

const traitementForm = reactive({
  dossier: null as any,
  typeDemande: 'Congé annuel',
  agent: '',
  matricule: '',
  note: '',
  dateEffet: store.todayISO,
  statut: "En cours d'analyse",
})

const nouvelleForm = reactive({
  typeDemande: 'Congé annuel',
  service: 'Direction Générale',
  agent: '',
  matricule: '',
  dateDebut: '',
  dateFin: '',
  description: '',
})

const wfSteps = computed(() => store.wfSteps)

const dossiersRH = computed(() => {
  const taskDossierIds = store.workflowTasks.map((t: any) => String(t.dossierId))
  return store.dossiers.filter((d: any) => taskDossierIds.includes(String(d.id)))
})

// Sync first dossier as default selection
watch(dossiersRH, (newVal) => {
  if (newVal.length > 0) {
    if (!traitementForm.dossier || !newVal.some((d: any) => d.id === traitementForm.dossier?.id)) {
      traitementForm.dossier = newVal[0]
    }
  } else {
    traitementForm.dossier = null
  }
}, { immediate: true })

// Methods
const openDossier = (d: any) => {
  selectedDossier.value = d
  traitementForm.dossier = d
  page.value = 'traiter'
  store.addToast('info', `Dossier RH ${d.ref} ouvert`)
}

const submitTraitement = async () => {
  if (traitementForm.dossier) {
    try {
      const note = [
        `Type: ${traitementForm.typeDemande}`,
        `Agent: ${traitementForm.agent}`,
        `Matricule: ${traitementForm.matricule}`,
        `Statut analyse: ${traitementForm.statut}`,
        traitementForm.note ? `Note: ${traitementForm.note}` : '',
      ].filter(Boolean).join('\n')
      await store.completeTraitementWorkflow(traitementForm.dossier.id, note, traitementForm.statut)
      store.addToast('success', 'Dossier transmis au DG pour validation')
      page.value = 'dashboard'
    } catch(e) {
      store.addToast('error', 'Erreur lors du traitement')
    }
  }
}

const submitNouvelleDemande = () => {
  page.value = 'dashboard'
  store.addToast('success', 'Demande RH créée et transmise !')
}

const addToast = (type: string, msg: string) => store.addToast(type, msg)

const openDocs = (d: any) => {
  const id = d?.id
  if (!id) {
    addToast('error', 'Dossier introuvable pour consulter les documents')
    return
  }
  store.openArchiveDetails(id)
}
</script>

<template>
  <div class="rh-view-wrapper">
    <div class="topbar">
      <div class="topbar-title">
        <span v-if="page === 'dashboard'">Tableau de bord — Service RH</span>
        <span v-if="page === 'traiter'">Traiter un dossier RH</span>
        <span v-if="page === 'nouveau'">Nouvelle demande RH</span>
      </div>
      <div class="topbar-actions">
        <button class="btn btn-primary btn-sm" @click="page = 'dashboard'">Retour au tableau de bord</button>
      </div>
    </div>

    <div class="content">
      <!-- Workflow bar -->
      <div class="workflow-bar">
        <div v-for="(s, i) in wfSteps" :key="i" class="wf-step" :class="{ done: s.done, active: s.active }">
          <div class="wf-circle">
            <span v-if="s.done">✓</span>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <div class="wf-label">{{ s.label }}</div>
        </div>
      </div>

      <!-- DASHBOARD RH -->
      <template v-if="page === 'dashboard'">
        <div class="page-header">
          <div class="page-title">Tableau de bord — Service RH</div>
          <div class="page-sub">Gestion des dossiers : congés, recrutements, formations, mutations</div>
        </div>
        <div class="stats-row">
          <div class="stat-card blue">
            <div class="stat-val" style="color: #3b82f6">{{ dossiersRH.length }}</div>
            <div class="stat-lbl">Dossiers reçus</div>
          </div>
          <div class="stat-card amber">
            <div class="stat-val" style="color: var(--amber)">4</div>
            <div class="stat-lbl">En traitement</div>
          </div>
          <div class="stat-card green">
            <div class="stat-val" style="color: var(--green)">8</div>
            <div class="stat-lbl">Traités</div>
          </div>
          <div class="stat-card red">
            <div class="stat-val" style="color: var(--red)">{{ dossiersRH.filter((d: any) => d.urgent).length }}</div>
            <div class="stat-lbl">Urgents</div>
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <div>
              <div class="card-title">Dossiers RH assignés</div>
              <div class="card-sub">Congés, recrutements, demandes personnel</div>
            </div>
            <button class="btn btn-primary btn-sm" @click="page = 'traiter'">Traiter un dossier</button>
          </div>
          <div v-if="dossiersRH.length === 0" class="empty-state-text">Aucun dossier RH en attente.</div>
          <table v-else>
            <thead>
              <tr>
                <th>Référence</th>
                <th>Objet</th>
                <th>Agent concerné</th>
                <th>Type RH</th>
                <th>Priorité</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dossiersRH" :key="d.id">
                <td><span class="td-ref">{{ d.numero }}</span></td>
                <td>
                  <div class="td-obj">{{ d.objet }}</div>
                  <div class="td-sub">{{ d.expediteur }}</div>
                </td>
                <td style="font-size: 12px">{{ d.service }}</td>
                <td><span class="badge b-type">RH</span></td>
                <td><span class="badge" :class="d.urgent ? 'b-urgent' : 'b-pending'">{{ d.priorite || (d.urgent ? 'Urgent' : 'Normal') }}</span></td>
                <td>
                  <div style="display: flex; gap: 6px">
                    <button class="btn btn-amber-soft btn-sm" @click="openDossier(d)">Traiter</button>
                    <button class="btn btn-outline btn-sm" @click="openDocs(d)">Consulter</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <!-- TRAITER DOSSIER RH -->
      <template v-if="page === 'traiter'">
        <div class="cols-2">
          <!-- Formulaire de traitement -->
          <div class="card">
            <div class="card-header">
              <div class="card-title">Informations du dossier</div>
            </div>
            <div class="card-body">
              <div class="form-grid" style="gap: 14px">
                <div class="form-group">
                  <label class="form-label">Dossier concerné<span class="form-required">*</span></label>
                  <select class="form-select" v-model="traitementForm.dossier">
                    <option v-for="d in dossiersRH" :key="d.id" :value="d">{{ d.numero }} — {{ d.objet }}</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">Type de demande RH</label>
                  <select class="form-select" v-model="traitementForm.typeDemande">
                    <option>Congé annuel</option>
                    <option>Congé maladie</option>
                    <option>Demande de recrutement</option>
                    <option>Note de service</option>
                    <option>Mutation</option>
                    <option>Formation</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">Agent concerné</label>
                  <input type="text" class="form-input" v-model="traitementForm.agent" placeholder="Nom et prénom de l'agent..." />
                </div>
                <div class="form-group">
                  <label class="form-label">Matricule</label>
                  <input type="text" class="form-input" v-model="traitementForm.matricule" placeholder="Matricule RH..." />
                </div>
                <div class="form-group" style="grid-column: 1 / -1">
                  <label class="form-label">Note de traitement</label>
                  <textarea class="form-textarea" v-model="traitementForm.note" placeholder="Observations et décision RH..."></textarea>
                </div>
                <div class="form-group">
                  <label class="form-label">Date de prise d'effet</label>
                  <input type="date" class="form-input" v-model="traitementForm.dateEffet" />
                </div>
                <div class="form-group">
                  <label class="form-label">Statut de traitement</label>
                  <select class="form-select" v-model="traitementForm.statut">
                    <option>En cours d'analyse</option>
                    <option>Approuvé</option>
                    <option>Refusé</option>
                    <option>Renvoyé DG</option>
                  </select>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
                <button class="btn btn-outline" v-if="traitementForm.dossier" @click="openDocs(traitementForm.dossier)">Voir documents</button>
                <button class="btn btn-primary" @click="submitTraitement">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                  Transmettre à validation
                </button>
              </div>
            </div>
          </div>

          <!-- Pièces justificatives -->
          <div class="card">
            <div class="card-header">
              <div class="card-title">Joindre pièces justificatives</div>
            </div>
            <div class="card-body">
              <div class="dropzone" @click="addToast('info', 'Sélection de fichier...')">
                <svg viewBox="0 0 24 24" fill="none" stroke-width="1.5">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
                <div class="dropzone-text">Glissez ou <span>parcourez</span></div>
                <div class="dropzone-hint">PDF, JPG, PNG — Max 10 Mo</div>
                <div class="dropzone-files" v-if="rhFiles.length">{{ rhFiles.length }} fichier(s) : {{ rhFiles.join(', ') }}</div>
              </div>
              <div style="margin-top: 14px">
                <div class="form-group">
                  <label class="form-label">Type de document</label>
                  <select class="form-select">
                    <option>Demande signée</option>
                    <option>Justificatif médical</option>
                    <option>Attestation</option>
                    <option>Décision</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- NOUVELLE DEMANDE RH -->
      <template v-if="page === 'nouveau'">
        <div class="page-header">
          <div class="page-title">Nouvelle demande RH</div>
        </div>
        <div class="card">
          <div class="card-header">
            <div class="card-title">Formulaire de demande — Service RH</div>
          </div>
          <div class="card-body">
            <div class="form-grid form-2" style="gap: 14px">
              <div class="form-group">
                <label class="form-label">Type de demande<span class="form-required">*</span></label>
                <select class="form-select" v-model="nouvelleForm.typeDemande">
                  <option>Congé annuel</option>
                  <option>Congé maladie</option>
                  <option>Recrutement</option>
                  <option>Formation</option>
                  <option>Mutation</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Service concerné<span class="form-required">*</span></label>
                <select class="form-select" v-model="nouvelleForm.service">
                  <option>Direction Générale</option>
                  <option>Service Technique</option>
                  <option>Service Juridique</option>
                  <option>Service Financier</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Agent concerné<span class="form-required">*</span></label>
                <input type="text" class="form-input" v-model="nouvelleForm.agent" placeholder="Nom et prénom..." />
              </div>
              <div class="form-group">
                <label class="form-label">Matricule</label>
                <input type="text" class="form-input" v-model="nouvelleForm.matricule" placeholder="Matricule RH..." />
              </div>
              <div class="form-group">
                <label class="form-label">Date début</label>
                <input type="date" class="form-input" v-model="nouvelleForm.dateDebut" />
              </div>
              <div class="form-group">
                <label class="form-label">Date fin</label>
                <input type="date" class="form-input" v-model="nouvelleForm.dateFin" />
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Description</label>
                <textarea class="form-textarea" v-model="nouvelleForm.description" placeholder="Détails de la demande..."></textarea>
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Pièces justificatives</label>
                <div class="dropzone" @click="addToast('info', 'Sélection...')">
                  <svg viewBox="0 0 24 24" fill="none" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  <div class="dropzone-text">Glissez ou <span>parcourez</span></div>
                  <div class="dropzone-hint">PDF, JPG, PNG — Max 10 Mo</div>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
              <button class="btn btn-primary" @click="submitNouvelleDemande">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                Soumettre la demande
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.rh-view-wrapper {
  --bg: #f5f6fa;
  --white: #ffffff;
  --border: #e2e5ed;
  --border2: #d0d4de;
  --text: #1a1d2e;
  --muted: #6b7280;
  --light: #9ca3af;
  --accent: #059669;
  --accent-s: #ecfdf5;
  --accent-h: #047857;
  --green: #059669;
  --green-s: #ecfdf5;
  --red: #dc2626;
  --red-s: #fef2f2;
  --amber: #d97706;
  --amber-s: #fffbeb;
  font-family: 'Inter', sans-serif;
  color: var(--text);
  min-height: 100%;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.content {
  padding: 24px;
}

/* === WORKFLOW BAR === */
.workflow-bar {
  background: white;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.wf-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.wf-step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 13px;
  left: 55%;
  width: 90%;
  height: 2px;
  background: var(--border);
  z-index: 0;
}

.wf-step.done:not(:last-child)::after {
  background: var(--accent);
}

.wf-circle {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  border: 2px solid var(--border);
  background: white;
  position: relative;
  z-index: 1;
  transition: all 0.3s;
}

.wf-step.done .wf-circle {
  background: var(--accent);
  border-color: var(--accent);
  color: white;
}

.wf-step.active .wf-circle {
  border-color: var(--accent);
  color: var(--accent);
  box-shadow: 0 0 0 4px rgba(5, 150, 105, 0.1);
}

.wf-label {
  font-size: 9px;
  margin-top: 6px;
  color: var(--muted);
  text-align: center;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.wf-step.done .wf-label {
  color: var(--accent);
}

.wf-step.active .wf-label {
  color: var(--text);
}

/* === PAGE HEADER === */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 4px;
}

.page-sub {
  font-size: 13px;
  color: var(--muted);
}

/* === STATS === */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--white);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 16px 18px;
  border-left: 4px solid transparent;
}

.stat-card.blue { border-left-color: #3b82f6; }
.stat-card.green { border-left-color: var(--green); }
.stat-card.amber { border-left-color: var(--amber); }
.stat-card.red { border-left-color: var(--red); }

.stat-val {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-lbl {
  font-size: 11px;
  color: var(--muted);
  font-weight: 500;
}

/* === CARD === */
.card {
  background: var(--white);
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 20px;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 14px;
  font-weight: 700;
}

.card-sub {
  font-size: 11px;
  color: var(--muted);
  margin-top: 2px;
}

.card-body {
  padding: 20px;
}

/* === GRID LAYOUTS === */
.cols-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

/* === FORMS === */
.form-grid {
  display: grid;
  gap: 16px;
}

.form-2 {
  grid-template-columns: 1fr 1fr;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-required {
  color: var(--red);
  margin-left: 2px;
}

.form-input,
.form-select,
.form-textarea {
  padding: 9px 12px;
  border: 1.5px solid var(--border);
  border-radius: 7px;
  font-size: 13px;
  font-family: 'Inter', sans-serif;
  color: var(--text);
  background: white;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.08);
}

.form-textarea {
  resize: vertical;
  min-height: 90px;
}

/* === DROPZONE === */
.dropzone {
  border: 2px dashed var(--border2);
  border-radius: 8px;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  background: #fafbfc;
  transition: all 0.2s;
}

.dropzone:hover {
  border-color: var(--accent);
  background: var(--accent-s);
}

.dropzone svg {
  width: 28px;
  height: 28px;
  stroke: var(--muted);
  fill: none;
  stroke-width: 1.5;
  margin: 0 auto 8px;
  display: block;
}

.dropzone-text {
  font-size: 13px;
  color: var(--muted);
}

.dropzone-text span {
  color: var(--accent);
  font-weight: 600;
}

.dropzone-hint {
  font-size: 11px;
  color: var(--light);
  margin-top: 3px;
}

.dropzone-files {
  font-size: 13px;
  color: var(--accent);
  font-weight: 600;
  margin-top: 6px;
}

/* === BUTTONS === */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1.5px solid transparent;
  font-family: 'Inter', sans-serif;
  transition: all 0.2s;
}

.btn svg {
  width: 14px;
  height: 14px;
}

.btn-primary {
  background: var(--accent);
  color: white;
  border-color: var(--accent);
}

.btn-primary:hover {
  background: var(--accent-h);
}

.btn-outline {
  background: white;
  color: var(--text);
  border-color: var(--border);
}

.btn-outline:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.btn-amber-soft {
  background: var(--amber-s);
  color: var(--amber);
  border-color: #fde68a;
}

.btn-amber-soft:hover {
  background: #fde68a;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

/* === TABLE === */
table {
  width: 100%;
  border-collapse: collapse;
}

th {
  padding: 10px 16px;
  text-align: left;
  font-size: 10px;
  font-weight: 700;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  background: #f8f9fb;
  border-bottom: 1px solid var(--border);
}

td {
  padding: 13px 16px;
  font-size: 13px;
  border-bottom: 1px solid #f1f3f7;
}

tr:hover td {
  background: #fafbfc;
}

.td-ref {
  font-weight: 700;
  font-size: 12px;
  color: var(--green);
}

.td-obj {
  font-weight: 600;
  font-size: 13px;
}

.td-sub {
  font-size: 11px;
  color: var(--muted);
  margin-top: 2px;
}

/* === BADGES === */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 600;
}

.badge::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.b-pending {
  background: #fffbeb;
  color: var(--amber);
}

.b-type {
  background: #eff6ff;
  color: #3b82f6;
}

.b-urgent {
  background: var(--red-s);
  color: var(--red);
}

.empty-state-text {
  padding: 24px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
</style>
