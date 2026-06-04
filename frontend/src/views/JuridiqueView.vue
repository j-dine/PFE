<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useAppStore } from '../stores/appStore'

const store = useAppStore()

// Page navigation synchronized with the Pinia store
const page = computed({
  get: () => {
    if (store.activeView === 'juridique-avis') return 'avis'
    if (store.activeView === 'juridique-contrats') return 'contrats'
    return 'dashboard'
  },
  set: (val: string) => {
    if (val === 'avis') store.activeView = 'juridique-avis'
    else if (val === 'contrats') store.activeView = 'juridique-contrats'
    else store.activeView = 'juridique-dashboard'
  }
})

// Local state
const selectedDossier = ref<any>(null)
const avisForm = reactive({
  dossier: null as any,
  nature: 'Avis contractuel',
  reference: '',
  conclusion: 'Avis favorable',
  analyse: '',
  recommandations: ''
})
const contratForm = reactive({
  intitule: '',
  partie: '',
  type: 'Marché public',
  echeance: '',
  observations: ''
})

const wfSteps = ['Réception', 'Enregistrement', 'Traitement', 'Validation', 'Paiement', 'Archivage']

const dossiersJuridique = ref([
  { id: 1, ref: 'CO-2026-0845', objet: 'Révision contrat fournisseur informatique', expediteur: 'Service Informatique', nature: 'Contractuel', delai: 'Demain 17h' },
  { id: 2, ref: 'CO-2026-0840', objet: 'Litige prestataire nettoyage', expediteur: 'Service Administratif', nature: 'Contentieux', delai: '03/06/2026' },
  { id: 3, ref: 'CO-2026-0836', objet: 'Conformité RGPD plateforme cloud', expediteur: 'Direction SI', nature: 'Réglementaire', delai: '10/06/2026' },
])

const contrats = ref([
  { id: 1, objet: 'Contrat maintenance informatique', partie: 'SARL TechSolutions', statut: 'En révision' },
  { id: 2, objet: 'Convention formation personnel', partie: 'CNEPD', statut: 'Validé' },
  { id: 3, objet: 'Marché nettoyage 2026', partie: 'ProNet SARL', statut: 'En révision' },
])

// Default dossier selection
watch(dossiersJuridique, (newVal) => {
  if (newVal.length > 0 && (!avisForm.dossier || !newVal.some((d: any) => d.id === avisForm.dossier?.id))) {
    avisForm.dossier = newVal[0]
  }
}, { immediate: true })

// Methods
const openAvis = (d: any) => {
  selectedDossier.value = d
  avisForm.dossier = d
  page.value = 'avis'
  store.addToast('info', `Dossier juridique ${d.ref} ouvert`)
}

const submitAvis = () => {
  page.value = 'dashboard'
  store.addToast('success', 'Avis juridique soumis et transmis !')
}

const addContract = () => {
  page.value = 'dashboard'
  store.addToast('success', 'Contrat enregistré pour révision !')
}

const addToast = (type: string, msg: string) => store.addToast(type, msg)

const getNatureBadgeStyle = (nature: string) => {
  if (nature === 'Contentieux') return { background: 'var(--red-s)', color: 'var(--red)' }
  if (nature === 'Réglementaire') return { background: 'var(--amber-s)', color: 'var(--amber)' }
  return { background: 'var(--accent-s)', color: 'var(--accent)' }
}
</script>

<template>
  <div class="juridique-view-wrapper">
    <div class="topbar">
      <div class="topbar-title">
        <span v-if="page === 'dashboard'">Tableau de bord — Service Juridique</span>
        <span v-if="page === 'avis'">Rédiger un avis juridique</span>
        <span v-if="page === 'contrats'">Gestion des contrats</span>
      </div>
      <div class="topbar-actions">
        <button class="btn btn-primary btn-sm" @click="page = 'dashboard'">Retour au tableau de bord</button>
      </div>
    </div>

    <div class="content">
      <!-- Workflow bar -->
      <div class="workflow-bar">
        <div v-for="(s, i) in wfSteps" :key="i" class="wf-step" :class="{ done: i < 2, active: i === 2 }">
          <div class="wf-circle">
            <span v-if="i < 2">✓</span>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <div class="wf-label">{{ s }}</div>
        </div>
      </div>

      <!-- DASHBOARD JURIDIQUE -->
      <template v-if="page === 'dashboard'">
        <div class="page-header">
          <div class="page-title">Tableau de bord — Service Juridique</div>
          <div class="page-sub">Suivi des avis juridiques, contentieux et contrats</div>
        </div>
        <div class="stats-row">
          <div class="stat-card purple">
            <div class="stat-val" style="color: var(--accent)">{{ dossiersJuridique.length }}</div>
            <div class="stat-lbl">Dossiers juridiques</div>
          </div>
          <div class="stat-card red">
            <div class="stat-val" style="color: var(--red)">{{ dossiersJuridique.filter(d => d.nature === 'Contentieux').length }}</div>
            <div class="stat-lbl">Contentieux actifs</div>
          </div>
          <div class="stat-card amber">
            <div class="stat-val" style="color: var(--amber)">6</div>
            <div class="stat-lbl">Avis en cours</div>
          </div>
          <div class="stat-card green">
            <div class="stat-val" style="color: var(--green)">21</div>
            <div class="stat-lbl">Dossiers clôturés</div>
          </div>
        </div>

        <div class="cols-2">
          <!-- Dossiers en attente d'avis -->
          <div class="card">
            <div class="card-header">
              <div>
                <div class="card-title">Dossiers en attente d'avis juridique</div>
              </div>
              <button class="btn btn-primary btn-sm" @click="page = 'avis'">Rédiger un avis</button>
            </div>
            <div v-if="dossiersJuridique.length === 0" class="empty-state-text">Aucun dossier en attente.</div>
            <table v-else>
              <thead>
                <tr>
                  <th>Référence</th>
                  <th>Objet</th>
                  <th>Nature</th>
                  <th>Délai</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in dossiersJuridique" :key="d.id">
                  <td><span class="td-ref">{{ d.ref }}</span></td>
                  <td>
                    <div class="td-obj">{{ d.objet }}</div>
                    <div class="td-sub">{{ d.expediteur }}</div>
                  </td>
                  <td>
                    <span class="badge" :style="getNatureBadgeStyle(d.nature)">{{ d.nature }}</span>
                  </td>
                  <td style="font-size: 11px; color: var(--red); font-weight: 600">{{ d.delai }}</td>
                  <td>
                    <button class="btn btn-purple-soft btn-sm" @click="openAvis(d)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                      Rédiger avis
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Contrats en cours -->
          <div class="card">
            <div class="card-header">
              <div class="card-title">Contrats en cours de révision</div>
              <button class="btn btn-outline btn-sm" @click="page = 'contrats'">Nouveau contrat</button>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Contrat</th>
                  <th>Partie</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in contrats" :key="c.id">
                  <td><div class="td-obj">{{ c.objet }}</div></td>
                  <td style="font-size: 12px; color: var(--muted)">{{ c.partie }}</td>
                  <td>
                    <span class="badge" :class="c.statut === 'Validé' ? 'b-validate' : 'b-pending'">{{ c.statut }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>

      <!-- RÉDIGER AVIS -->
      <template v-if="page === 'avis'">
        <div class="page-header">
          <div class="page-title">Rédiger un avis juridique</div>
        </div>
        <div class="card">
          <div class="card-header">
            <div class="card-title">Formulaire d'avis juridique</div>
          </div>
          <div class="card-body">
            <div class="form-grid form-2" style="gap: 14px">
              <div class="form-group">
                <label class="form-label">Dossier concerné<span class="form-required">*</span></label>
                <select class="form-select" v-model="avisForm.dossier">
                  <option v-for="d in dossiersJuridique" :key="d.id" :value="d">{{ d.ref }} — {{ d.objet }}</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Nature juridique<span class="form-required">*</span></label>
                <select class="form-select" v-model="avisForm.nature">
                  <option>Avis contractuel</option>
                  <option>Contentieux</option>
                  <option>Conformité légale</option>
                  <option>Consultation réglementaire</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Textes de référence</label>
                <input type="text" class="form-input" v-model="avisForm.reference" placeholder="Loi, décret, article..." />
              </div>
              <div class="form-group">
                <label class="form-label">Conclusion</label>
                <select class="form-select" v-model="avisForm.conclusion">
                  <option>Avis favorable</option>
                  <option>Avis défavorable</option>
                  <option>Avis avec réserves</option>
                  <option>Nécessite complément</option>
                </select>
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Analyse juridique<span class="form-required">*</span></label>
                <textarea class="form-textarea" v-model="avisForm.analyse" style="min-height: 120px" placeholder="Développez l'analyse juridique du dossier..."></textarea>
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Recommandations</label>
                <textarea class="form-textarea" v-model="avisForm.recommandations" placeholder="Recommandations et mesures à prendre..."></textarea>
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Documents annexes</label>
                <div class="dropzone" @click="addToast('info', 'Sélection fichier...')">
                  <svg viewBox="0 0 24 24" fill="none" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  <div class="dropzone-text">Glissez ou <span>parcourez</span></div>
                  <div class="dropzone-hint">PDF, DOC — Max 10 Mo</div>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
              <button class="btn btn-purple-soft" @click="addToast('info', 'Avis sauvegardé en brouillon')">Brouillon</button>
              <button class="btn btn-primary" @click="submitAvis">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                Soumettre l'avis
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- CONTRATS -->
      <template v-if="page === 'contrats'">
        <div class="page-header">
          <div class="page-title">Gestion des contrats</div>
        </div>
        <div class="card">
          <div class="card-header">
            <div class="card-title">Nouveau contrat à réviser</div>
          </div>
          <div class="card-body">
            <div class="form-grid form-2" style="gap: 14px">
              <div class="form-group">
                <label class="form-label">Intitulé du contrat<span class="form-required">*</span></label>
                <input type="text" class="form-input" v-model="contratForm.intitule" placeholder="Titre du contrat..." />
              </div>
              <div class="form-group">
                <label class="form-label">Partie contractante<span class="form-required">*</span></label>
                <input type="text" class="form-input" v-model="contratForm.partie" placeholder="Nom de l'entité..." />
              </div>
              <div class="form-group">
                <label class="form-label">Type de contrat</label>
                <select class="form-select" v-model="contratForm.type">
                  <option>Marché public</option>
                  <option>Convention</option>
                  <option>Accord-cadre</option>
                  <option>Contrat de prestation</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Date d'échéance</label>
                <input type="date" class="form-input" v-model="contratForm.echeance" />
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Observations juridiques</label>
                <textarea class="form-textarea" v-model="contratForm.observations" placeholder="Remarques sur le contrat..."></textarea>
              </div>
              <div class="form-group" style="grid-column: 1 / -1">
                <label class="form-label">Fichier contrat</label>
                <div class="dropzone" @click="addToast('info', 'Sélection...')">
                  <svg viewBox="0 0 24 24" fill="none" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  <div class="dropzone-text">Glissez ou <span>parcourez</span></div>
                  <div class="dropzone-hint">PDF, DOC — Max 10 Mo</div>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-outline" @click="page = 'dashboard'">Annuler</button>
              <button class="btn btn-primary" @click="addContract">Enregistrer</button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.juridique-view-wrapper {
  --bg: #f5f6fa;
  --white: #ffffff;
  --border: #e2e5ed;
  --border2: #d0d4de;
  --text: #1a1d2e;
  --muted: #6b7280;
  --light: #9ca3af;
  --accent: #7c3aed;
  --accent-s: #f5f3ff;
  --accent-h: #6d28d9;
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
.wf-step.active .wf-circle { border-color: var(--accent); color: var(--accent); box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.1); }

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

.stat-card.purple { border-left-color: var(--accent); }
.stat-card.red { border-left-color: var(--red); }
.stat-card.amber { border-left-color: var(--amber); }
.stat-card.green { border-left-color: var(--green); }

.stat-val { font-size: 26px; font-weight: 700; line-height: 1; margin-bottom: 4px; }
.stat-lbl { font-size: 11px; color: var(--muted); font-weight: 500; }

/* === LAYOUT === */
.cols-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

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
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.08);
}

.form-textarea { resize: vertical; min-height: 90px; }

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
.btn-purple-soft { background: var(--accent-s); color: var(--accent); border-color: #ddd6fe; }
.btn-purple-soft:hover { background: #ddd6fe; }
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
.b-validate { background: var(--green-s); color: var(--green); }

.empty-state-text {
  padding: 24px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
</style>
