<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useAppStore } from '../stores/appStore'

const store = useAppStore()

// Page navigation synchronized with the Pinia store
const page = computed({
  get: () => {
    if (store.activeView === 'technique-rapport') return 'rapport'
    if (store.activeView === 'technique-demande') return 'demande'
    return 'dashboard'
  },
  set: (val: string) => {
    if (val === 'rapport') store.activeView = 'technique-rapport'
    else if (val === 'demande') store.activeView = 'technique-demande'
    else store.activeView = 'technique-dashboard'
  }
})

// Local state
const selectedDossier = ref<any>(null)
const techFiles = ref<string[]>([])

const rapportForm = reactive({
  dossier: null as any,
  domaine: 'Infrastructure',
  ingenieur: '',
  inspectionDate: '2026-05-28',
  constatations: '',
  recommandations: '',
  estimation: '',
  delai: '1 semaine',
  avis: 'Favorable',
  urgence: 'Normal',
})

const demandeForm = reactive({
  objet: '',
  type: 'Expertise technique',
  serviceDemandeur: 'Direction Générale',
  localisation: '',
  dateSouhaitee: '',
  description: '',
  priorite: 'Normal',
})

const wfSteps = computed(() => store.wfSteps)

const dossiersTech = computed(() => {
  const taskDossierIds = store.workflowTasks.map((t: any) => String(t.dossierId))
  return store.dossiers.filter((d: any) => taskDossierIds.includes(String(d.id)))
})

// Default dossier selection
watch(dossiersTech, (newVal) => {
  if (newVal.length > 0 && (!rapportForm.dossier || !newVal.some((d: any) => d.id === rapportForm.dossier?.id))) {
    rapportForm.dossier = newVal[0]
  }
}, { immediate: true })

// Methods
const openRapport = (d: any) => {
  selectedDossier.value = d
  rapportForm.dossier = d
  page.value = 'rapport'
  store.addToast('info', `Dossier technique ${d.ref} ouvert`)
}

const submitRapport = async () => {
  if (rapportForm.dossier) {
    try {
      const note = `Constatations: ${rapportForm.constatations}\nRecommandations: ${rapportForm.recommandations}\nEstimation: ${rapportForm.estimation}\nDélai: ${rapportForm.delai}`
      await store.completeTraitementWorkflow(rapportForm.dossier.id, note, rapportForm.avis)
      store.addToast('success', 'Rapport transmis au DG pour validation')
      page.value = 'dashboard'
    } catch(e) {
      store.addToast('error', 'Erreur lors du traitement')
    }
  }
}

const submitDemande = () => {
  page.value = 'dashboard'
  store.addToast('success', 'Demande technique envoyée !')
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
  <div class="technique-view-wrapper">
    <div class="topbar">
      <div class="topbar-title">
        <span v-if="page === 'dashboard'">Tableau de bord — Service Technique</span>
        <span v-if="page === 'rapport'">Rapport technique</span>
        <span v-if="page === 'demande'">Demande technique</span>
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

      <!-- DASHBOARD TECHNIQUE -->
      <template v-if="page === 'dashboard'">
        <div class="page-header">
          <div class="page-title">Tableau de bord — Service Technique</div>
          <div class="page-sub">Études, rapports d'expertise et demandes d'intervention</div>
        </div>
        <div class="stats-row">
          <div class="stat-card blue">
            <div class="stat-val" style="color: #3b82f6">{{ dossiersTech.length }}</div>
            <div class="stat-lbl">Dossiers assignés</div>
          </div>
          <div class="stat-card amber">
            <div class="stat-val" style="color: var(--accent)">6</div>
            <div class="stat-lbl">En cours d'étude</div>
          </div>
          <div class="stat-card green">
            <div class="stat-val" style="color: var(--green)">9</div>
            <div class="stat-lbl">Rapports rendus</div>
          </div>
          <div class="stat-card red">
            <div class="stat-val" style="color: var(--red)">{{ dossiersTech.filter((d: any) => d.urgent).length }}</div>
            <div class="stat-lbl">Urgents</div>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div>
              <div class="card-title">Dossiers techniques à traiter</div>
              <div class="card-sub">Études, rapports et expertises techniques</div>
            </div>
            <button class="btn btn-primary btn-sm" @click="page = 'rapport'">Nouveau rapport</button>
          </div>
          <div v-if="dossiersTech.length === 0" class="empty-state-text">Aucun dossier technique en attente.</div>
          <table v-else>
            <thead>
              <tr>
                <th>Référence</th>
                <th>Objet</th>
                <th>Domaine</th>
                <th>Priorité</th>
                <th>Délai</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dossiersTech" :key="d.id">
                <td><span class="td-ref">{{ d.numero }}</span></td>
                <td>
                  <div class="td-obj">{{ d.objet }}</div>
                  <div class="td-sub">{{ d.expediteur }}</div>
                </td>
                <td><span class="badge b-domain">Technique</span></td>
                <td><span class="badge" :class="d.urgent ? 'b-urgent' : 'b-pending'">{{ d.priorite || (d.urgent ? 'Urgent' : 'Normal') }}</span></td>
                <td style="font-size: 11px; color: var(--red); font-weight: 600">{{ d.deadline || d.delai }}</td>
                <td>
                  <div style="display: flex; gap: 6px">
                    <button class="btn btn-primary btn-sm" @click="openRapport(d)">Traiter</button>
                    <button class="btn btn-outline btn-sm" @click="openDocs(d)">Consulter</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <!-- RAPPORT TECHNIQUE -->
      <template v-if="page === 'rapport'">
        <div class="page-header">
          <div class="page-title">Rapport technique</div>
        </div>
        <div class="card">
          <div class="card-header">
            <div class="card-title">Formulaire — Rapport technique</div>
          </div>
          <div class="card-body">
            <div class="form-grid form-2" style="gap: 14px">
              <div class="form-group">
                <label class="form-label">Dossier concerné<span class="form-required">*</span></label>
                <select class="form-select" v-model="rapportForm.dossier">
                  <option v-for="d in dossiersTech" :key="d.id" :value="d">{{ d.numero }} — {{ d.objet }}</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Domaine technique<span class="form-required">*</span></label>
                <select class="form-select" v-model="rapportForm.domaine">
                  <option>Infrastructure</option>
                  <option>Informatique</option>
                  <option>Travaux &amp; BTP</option>
                  <option>Équipements</option>
                  <option>Maintenance</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Ingénieur responsable</label>
                <input type="text" class="form-input" v-model="rapportForm.ingenieur" placeholder="Nom de l'ingénieur..." />
              </div>
              <div class="form-group">
                <label class="form-label">Date d'inspection / visite</label>
                <input type="date" class="form-input" v-model="rapportForm.inspectionDate" />
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Constatations techniques<span class="form-required">*</span></label>
                <textarea class="form-textarea" v-model="rapportForm.constatations" style="min-height: 110px" placeholder="Décrivez les constatations lors de l'inspection..."></textarea>
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Recommandations &amp; Solutions proposées</label>
                <textarea class="form-textarea" v-model="rapportForm.recommandations" placeholder="Solutions techniques recommandées..."></textarea>
              </div>
              <div class="form-group">
                <label class="form-label">Estimation budgétaire (DA)</label>
                <input type="number" class="form-input" v-model="rapportForm.estimation" placeholder="Montant estimé..." />
              </div>
              <div class="form-group">
                <label class="form-label">Délai d'exécution estimé</label>
                <select class="form-select" v-model="rapportForm.delai">
                  <option>1 semaine</option>
                  <option>2 semaines</option>
                  <option>1 mois</option>
                  <option>3 mois</option>
                  <option>6 mois</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Avis technique</label>
                <div class="priority-group">
                  <div class="priority-opt" :class="{ active: rapportForm.avis === 'Favorable' }" @click="rapportForm.avis = 'Favorable'">Favorable</div>
                  <div class="priority-opt urgent" :class="{ active: rapportForm.avis === 'Défavorable' }" @click="rapportForm.avis = 'Défavorable'">Défavorable</div>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Niveau d'urgence</label>
                <div class="priority-group">
                  <div class="priority-opt" :class="{ active: rapportForm.urgence === 'Normal' }" @click="rapportForm.urgence = 'Normal'">Normal</div>
                  <div class="priority-opt urgent" :class="{ active: rapportForm.urgence === 'Urgent' }" @click="rapportForm.urgence = 'Urgent'">Urgent</div>
                </div>
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Photos &amp; Documents techniques</label>
                <div class="dropzone" @click="addToast('info', 'Sélection...')">
                  <svg viewBox="0 0 24 24" fill="none" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  <div class="dropzone-text">Glissez ou <span>parcourez</span></div>
                  <div class="dropzone-hint">PDF, JPG, PNG, DWG — Max 10 Mo</div>
                  <div class="dropzone-files" v-if="techFiles.length">{{ techFiles.length }} fichier(s) : {{ techFiles.join(', ') }}</div>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
              <button class="btn btn-outline" v-if="rapportForm.dossier" @click="openDocs(rapportForm.dossier)">Voir documents</button>
              <button class="btn btn-amber-soft" @click="addToast('info', 'Rapport sauvegardé en brouillon')">Brouillon</button>
              <button class="btn btn-primary" @click="submitRapport">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                Soumettre le rapport
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- DEMANDE TECHNIQUE -->
      <template v-if="page === 'demande'">
        <div class="page-header">
          <div class="page-title">Demande technique</div>
        </div>
        <div class="card">
          <div class="card-header">
            <div class="card-title">Formulaire de demande technique</div>
          </div>
          <div class="card-body">
            <div class="form-grid form-2" style="gap: 14px">
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Objet de la demande<span class="form-required">*</span></label>
                <input type="text" class="form-input" v-model="demandeForm.objet" placeholder="Décrivez l'objet de la demande technique..." />
              </div>
              <div class="form-group">
                <label class="form-label">Type d'intervention<span class="form-required">*</span></label>
                <select class="form-select" v-model="demandeForm.type">
                  <option>Maintenance corrective</option>
                  <option>Maintenance préventive</option>
                  <option>Expertise technique</option>
                  <option>Étude de faisabilité</option>
                  <option>Installation</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Service demandeur</label>
                <select class="form-select" v-model="demandeForm.serviceDemandeur">
                  <option>Direction Générale</option>
                  <option>Service RH</option>
                  <option>Service Juridique</option>
                  <option>Service Financier</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Localisation / Site</label>
                <input type="text" class="form-input" v-model="demandeForm.localisation" placeholder="Adresse ou bâtiment..." />
              </div>
              <div class="form-group">
                <label class="form-label">Délai souhaité</label>
                <input type="date" class="form-input" v-model="demandeForm.dateSouhaitee" />
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Description détaillée</label>
                <textarea class="form-textarea" v-model="demandeForm.description" style="min-height: 100px" placeholder="Décrivez en détail la nature de l'intervention..."></textarea>
              </div>
              <div class="form-group">
                <label class="form-label">Priorité</label>
                <div class="priority-group">
                  <div class="priority-opt" :class="{ active: demandeForm.priorite === 'Normal' }" @click="demandeForm.priorite = 'Normal'">Normal</div>
                  <div class="priority-opt urgent" :class="{ active: demandeForm.priorite === 'Urgent' }" @click="demandeForm.priorite = 'Urgent'">Urgent</div>
                  <div class="priority-opt urgent" :class="{ active: demandeForm.priorite === 'Très urgent' }" @click="demandeForm.priorite = 'Très urgent'">Très urgent</div>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Photos du problème</label>
                <div class="dropzone" @click="addToast('info', 'Sélection...')">
                  <svg viewBox="0 0 24 24" fill="none" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  <div class="dropzone-text">Glissez ou <span>parcourez</span></div>
                  <div class="dropzone-hint">JPG, PNG — Max 10 Mo</div>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
              <button class="btn btn-primary" @click="submitDemande">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                Envoyer la demande
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.technique-view-wrapper {
  --bg: #f5f6fa;
  --white: #ffffff;
  --border: #e2e5ed;
  --border2: #d0d4de;
  --text: #1a1d2e;
  --muted: #6b7280;
  --light: #9ca3af;
  --accent: #d97706;
  --accent-s: #fffbeb;
  --accent-h: #b45309;
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

.content { padding: 24px; }

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

.wf-step.done:not(:last-child)::after { background: var(--accent); }

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

.wf-step.done .wf-circle { background: var(--accent); border-color: var(--accent); color: white; }
.wf-step.active .wf-circle { border-color: var(--accent); color: var(--accent); box-shadow: 0 0 0 4px rgba(217, 119, 6, 0.1); }

.wf-label {
  font-size: 9px;
  margin-top: 6px;
  color: var(--muted);
  text-align: center;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.wf-step.done .wf-label { color: var(--accent); }
.wf-step.active .wf-label { color: var(--text); }

/* === PAGE HEADER === */
.page-header { margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--text); margin-bottom: 4px; }
.page-sub { font-size: 13px; color: var(--muted); }

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
.stat-card.amber { border-left-color: var(--amber); }
.stat-card.green { border-left-color: var(--green); }
.stat-card.red { border-left-color: var(--red); }

.stat-val { font-size: 26px; font-weight: 700; line-height: 1; margin-bottom: 4px; }
.stat-lbl { font-size: 11px; color: var(--muted); font-weight: 500; }

/* === CARD === */
.card { background: var(--white); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; margin-bottom: 20px; }
.card-header { padding: 16px 20px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 14px; font-weight: 700; }
.card-sub { font-size: 11px; color: var(--muted); margin-top: 2px; }
.card-body { padding: 20px; }

/* === FORMS === */
.form-grid { display: grid; gap: 16px; }
.form-2 { grid-template-columns: 1fr 1fr; }
.form-group { display: flex; flex-direction: column; gap: 6px; }

.form-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-required { color: var(--red); margin-left: 2px; }

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
  box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.08);
}

.form-textarea { resize: vertical; min-height: 90px; }

/* === PRIORITY GROUP === */
.priority-group { display: flex; gap: 8px; }

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

.priority-opt.active { border-color: var(--accent); background: var(--accent-s); color: var(--accent); }
.priority-opt.urgent.active { border-color: var(--red); background: var(--red-s); color: var(--red); }

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

.dropzone:hover { border-color: var(--accent); background: var(--accent-s); }

.dropzone svg {
  width: 28px;
  height: 28px;
  stroke: var(--muted);
  fill: none;
  stroke-width: 1.5;
  margin: 0 auto 8px;
  display: block;
}

.dropzone-text { font-size: 13px; color: var(--muted); }
.dropzone-text span { color: var(--accent); font-weight: 600; }
.dropzone-hint { font-size: 11px; color: var(--light); margin-top: 3px; }
.dropzone-files { font-size: 13px; color: var(--accent); font-weight: 600; margin-top: 6px; }

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

.btn svg { width: 14px; height: 14px; }

.btn-primary { background: var(--accent); color: white; border-color: var(--accent); }
.btn-primary:hover { background: var(--accent-h); }
.btn-outline { background: white; color: var(--text); border-color: var(--border); }
.btn-outline:hover { border-color: var(--accent); color: var(--accent); }
.btn-amber-soft { background: var(--amber-s); color: var(--amber); border-color: #fde68a; }
.btn-amber-soft:hover { background: #fde68a; }
.btn-sm { padding: 6px 12px; font-size: 12px; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

/* === TABLE === */
table { width: 100%; border-collapse: collapse; }

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

td { padding: 13px 16px; font-size: 13px; border-bottom: 1px solid #f1f3f7; }
tr:hover td { background: #fafbfc; }

.td-ref { font-weight: 700; font-size: 12px; color: var(--accent); }
.td-obj { font-weight: 600; font-size: 13px; }
.td-sub { font-size: 11px; color: var(--muted); margin-top: 2px; }

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

.badge::before { content: ''; width: 5px; height: 5px; border-radius: 50%; background: currentColor; }

.b-pending { background: #fffbeb; color: var(--amber); }
.b-domain { background: #eff6ff; color: #3b82f6; }
.b-urgent { background: var(--red-s); color: var(--red); }

.empty-state-text {
  padding: 24px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
</style>
