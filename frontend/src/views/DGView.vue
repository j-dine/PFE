<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useAppStore } from '../stores/appStore'

const store = useAppStore()

// State
const selectedDossier = ref<any>(null)
const prio = ref('Normal')
const decision = ref('Approuvé')
const requiresPayment = ref(false)
const commentaire = ref('')

const reportForm = reactive({
  from: '2026-04-01',
  to: '2026-05-29',
})

const wfSteps = computed(() => store.wfSteps)

const dossiersAll = computed(() => {
  const taskDossierIds = store.workflowTasks.map((t: any) => String(t.dossierId))
  return store.dossiers.filter((d: any) => taskDossierIds.includes(String(d.id)))
})

// Watcher to automatically select the first dossier if none is selected
watch(dossiersAll, (newVal) => {
  if (newVal.length > 0) {
    if (!selectedDossier.value || !newVal.some((d: any) => d.id === selectedDossier.value.id)) {
      selectedDossier.value = newVal[0]
    }
  } else {
    selectedDossier.value = null
  }
}, { immediate: true })

// Computed property for page navigation synchronized with the Pinia store
const page = computed({
  get: () => {
    if (store.activeView === 'dg-valider') return 'valider'
    if (store.activeView === 'dg-rapports') return 'rapports'
    return 'dashboard'
  },
  set: (val: string) => {
    if (val === 'valider') store.activeView = 'dg-valider'
    else if (val === 'rapports') store.activeView = 'dg-rapports'
    else store.activeView = 'dg-dashboard'
  }
})

// Methods
const mapDecision = (label: string): 'APPROUVE' | 'REJETE' | 'COMPLEMENT' => {
  const v = String(label || '').toLowerCase()
  if (v.includes('rejet')) return 'REJETE'
  if (v.includes('compl')) return 'COMPLEMENT'
  return 'APPROUVE'
}

const valider = async (d: any) => {
  try {
    await store.completeValidationWorkflow(d.id, 'APPROUVE', false, '')
    toast('success', d.numero + ' approuvé — transmission archivage ou paiement')
  } catch(e) {
    toast('error', 'Erreur lors de la validation')
  }
}

const rejeter = async (d: any) => {
  try {
    await store.completeValidationWorkflow(d.id, 'REJETE', false, '')
    toast('info', d.numero + ' rejeté')
  } catch(e) {
    toast('error', 'Erreur lors du rejet')
  }
}

const validerEtTransmettre = async () => {
  if (selectedDossier.value) {
    const refNum = selectedDossier.value.numero
    try {
      const dgDecision = mapDecision(decision.value)
      await store.completeValidationWorkflow(
        selectedDossier.value.id,
        dgDecision,
        dgDecision === 'APPROUVE' && requiresPayment.value,
        commentaire.value,
      )
      toast('success', `${refNum} — décision DG enregistrée`)
      selectedDossier.value = null
    } catch(e) {
      toast('error', 'Erreur lors de la validation hiérarchique')
    }
  }
  page.value = 'dashboard'
}

const telechargerRapport = () => {
  toast('success', 'Rapport téléchargé')
}

const toast = (type: string, msg: string) => {
  store.addToast(type, msg)
}

const openDocs = (d: any) => {
  const id = d?.id
  if (!id) {
    toast('error', 'Dossier introuvable pour consulter les documents')
    return
  }
  store.openArchiveDetails(id)
}
</script>

<template>
  <div class="dg-view-wrapper">
    <div class="topbar">
      <div class="topbar-title">
        <span v-if="page === 'dashboard'">Tableau de bord — Direction Générale</span>
        <span v-if="page === 'valider'">Valider un dossier</span>
        <span v-if="page === 'rapports'">Générer un rapport</span>
      </div>
      <div class="topbar-actions">
        <button class="btn btn-primary btn-sm" @click="page = 'dashboard'">Retour au tableau de bord</button>
      </div>
    </div>

    <div class="content">
      <div class="workflow-bar">
        <div v-for="(s, i) in wfSteps" :key="i" class="wf-step" :class="{ done: s.done, active: s.active }">
          <div class="wf-circle">
            <span v-if="s.done">✓</span>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <div class="wf-label">{{ s.label }}</div>
        </div>
      </div>

      <!-- DASHBOARD DG -->
      <template v-if="page === 'dashboard'">
        <div class="page-header">
          <div class="page-title">Tableau de bord — Direction Générale</div>
          <div class="page-sub">Vue d'ensemble des dossiers et décisions en attente</div>
        </div>
        <div class="stats-row">
          <div class="stat-card blue">
            <div class="stat-val" style="color: var(--accent)">{{ dossiersAll.length }}</div>
            <div class="stat-lbl">Dossiers à valider</div>
          </div>
          <div class="stat-card green">
            <div class="stat-val" style="color: var(--green)">142</div>
            <div class="stat-lbl">Validés ce mois</div>
          </div>
          <div class="stat-card red">
            <div class="stat-val" style="color: var(--red)">{{ dossiersAll.filter((d: any) => d.urgent).length }}</div>
            <div class="stat-lbl">Urgents en attente</div>
          </div>
          <div class="stat-card amber">
            <div class="stat-val" style="color: var(--amber)">89%</div>
            <div class="stat-lbl">Taux de traitement</div>
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <div>
              <div class="card-title">Dossiers en attente de décision</div>
            </div>
            <button class="btn btn-primary btn-sm" @click="page = 'valider'">Valider un dossier</button>
          </div>
          <div v-if="dossiersAll.length === 0" class="empty-state-text">
            Aucun dossier en attente de décision.
          </div>
          <table v-else>
            <thead>
              <tr>
                <th>Référence</th>
                <th>Objet</th>
                <th>Service</th>
                <th>Priorité</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dossiersAll" :key="d.id">
                <td><span class="td-ref">{{ d.numero }}</span></td>
                <td>
                  <div class="td-obj">{{ d.objet }}</div>
                  <div class="td-sub">{{ d.expediteur }}</div>
                </td>
                <td style="font-size: 12px; color: var(--muted)">{{ d.service }}</td>
                <td><span class="badge" :class="d.urgent ? 'b-urgent' : 'b-new'">{{ d.priorite || (d.urgent ? 'Urgent' : 'Normal') }}</span></td>
                <td><span class="badge b-pending">En attente</span></td>
                <td>
                  <div style="display: flex; gap: 6px">
                    <button class="btn btn-success btn-sm" @click="valider(d)">✓ Valider</button>
                    <button class="btn btn-danger-soft btn-sm" @click="rejeter(d)">✗ Rejeter</button>
                    <button class="btn btn-outline btn-sm" @click="openDocs(d)">Consulter</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <!-- VALIDER DOSSIER -->
      <template v-if="page === 'valider'">
        <div class="page-header"><div class="page-title">Validation hiérarchique (DG)</div></div>
        <div class="card">
          <div class="card-body">
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label">Référence dossier</label>
                <select class="form-select" v-model="selectedDossier">
                  <option v-for="d in dossiersAll" :key="d.id" :value="d">{{ d.numero }} — {{ d.objet }}</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Décision (responsable hiérarchique — DG)</label>
                <select class="form-select" v-model="decision">
                  <option>Approuvé</option>
                  <option>Rejeté</option>
                  <option>Demande de complément</option>
                </select>
              </div>
              <div class="form-group" v-if="decision === 'Approuvé'">
                <label class="form-label">
                  <input type="checkbox" v-model="requiresPayment" style="margin-right:8px">
                  Nécessite un règlement financier
                </label>
              </div>
              <div class="form-group">
                <label class="form-label">Commentaire</label>
                <textarea class="form-textarea" v-model="commentaire" placeholder="Justifiez votre décision..."></textarea>
              </div>
              <div class="form-group">
                <label class="form-label">Niveau d'urgence</label>
                <div class="priority-group">
                  <div class="priority-opt" :class="{ active: prio === 'Normal' }" @click="prio = 'Normal'">Normal</div>
                  <div class="priority-opt urgent" :class="{ active: prio === 'Urgent' }" @click="prio = 'Urgent'">Urgent</div>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
              <button class="btn btn-outline" v-if="selectedDossier" @click="openDocs(selectedDossier)">Voir documents</button>
              <button class="btn btn-primary" @click="validerEtTransmettre">Valider & Transmettre</button>
            </div>
          </div>
        </div>
      </template>

      <!-- RAPPORTS -->
      <template v-if="page === 'rapports'">
        <div class="page-header"><div class="page-title">Générer un rapport</div></div>
        <div class="card">
          <div class="card-body">
            <div class="form-grid form-2">
              <div class="form-group">
                <label class="form-label">Du</label>
                <input type="date" class="form-input" v-model="reportForm.from" />
              </div>
              <div class="form-group">
                <label class="form-label">Au</label>
                <input type="date" class="form-input" v-model="reportForm.to" />
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
              <button class="btn btn-primary" @click="telechargerRapport">Télécharger</button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.dg-view-wrapper {
  --bg: #f5f6fa;
  --white: #ffffff;
  --border: #e2e5ed;
  --border2: #d0d4de;
  --text: #1a1d2e;
  --muted: #6b7280;
  --light: #9ca3af;
  --accent: #1d4ed8;
  --accent-s: #eff6ff;
  --accent-h: #1e40af;
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
  box-shadow: 0 0 0 4px rgba(29, 78, 216, 0.1);
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

.stat-card.blue {
  border-left-color: var(--accent);
}

.stat-card.green {
  border-left-color: var(--green);
}

.stat-card.amber {
  border-left-color: var(--amber);
}

.stat-card.red {
  border-left-color: var(--red);
}

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
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.08);
}

.form-textarea {
  resize: vertical;
  min-height: 90px;
}

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

.btn-success {
  background: var(--green);
  color: white;
  border-color: var(--green);
}

.btn-danger-soft {
  background: var(--red-s);
  color: var(--red);
  border-color: #fecaca;
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

.b-new {
  background: var(--accent-s);
  color: var(--accent);
}

.b-urgent {
  background: var(--red-s);
  color: var(--red);
}

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
  color: var(--accent);
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

.priority-group {
  display: flex;
  gap: 8px;
}

.priority-opt {
  flex: 1;
  padding: 8px 6px;
  border-radius: 7px;
  border: 1.5px solid var(--border);
  background: var(--bg);
  color: var(--muted);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s;
}

.priority-opt.active {
  border-color: var(--accent);
  background: var(--accent-s);
  color: var(--accent);
}

.priority-opt.urgent.active {
  border-color: var(--red);
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
