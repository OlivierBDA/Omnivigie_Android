### Prompt pour Agent de Codage (Google Antigravity)

**Sujet :** Refonte UI/UX de l'écran "Dashboard" de l'application Android Omnivigie (Jetpack Compose).

**Objectif :** Transformer le Dashboard actuel en une véritable tour de contrôle opérationnelle pour la veille, permettant de visualiser les données clés d'un coup d'œil et de déclencher les actions rapidement.

---

#### 1. Consignes de Modification de l'Écran Dashboard (`DashboardScreen.kt`)

Veuillez restructurer la disposition verticale du Dashboard selon l'ordre ci-dessous :

1. **Header / Statut Système :**
   * Conserver le message d'accueil personnalisé (`Bonjour, Olivier`).
   * Ajouter sous le titre la **date et l'heure de la dernière récupération des emails** (ex: *"Dernière synchro : Aujourd'hui à 08:30"*).
   * Condenser la section "Diagnostics Système" : au lieu de 3 grandes cartes verticales, regrouper les 3 états (Gmail API, Gemini SDK, NotebookLM) dans une seule carte horizontale compacte ou une ligne de badges/pills d'état avec pastilles de couleur (vert/rouge).

2. **Section d'Action Principale :**
   * Proposer un bouton d'action principal et bien mis en avant (Bouton d'action large / Primary Button) pour déclencher le pipeline complet (ou deux boutons séparés compacts "Sync Gmail" et "Qualifier LLM").

3. **Section "Statistiques & KPIs" (Cartes de métriques) :**
   * Réorganiser la grille de statistiques avec des cartes claires :
     * **Articles non qualifiés** (Articles récupérés en attente de passage LLM).
     * **Articles qualifiés en attente de Notebook** (Articles traités par le LLM mais non encore exportés/ajoutés dans un Notebook).
     * **Total Notebooks créés**.

4. **Section "Derniers Notebooks Générés" :**
   * Ajouter une liste/carte présentant les **2 ou 3 derniers Notebooks créés**.
   * Chaque élément de la liste doit afficher :
     * Le nom du Notebook (qui contient la date, ex: `Notebook_2026-07-20`).
     * Le thème principal ou le nombre d'articles associés.
     * Un icône à droite permettant d'ouvrir la fiche ou le lien associé.

---

#### 2. Directives Design System & Jetpack Compose
* Conservons le thème sombre (Dark Theme) et la palette de couleurs actuelle (violet/pourpre `#6750A4`, fond noir/sombre, cartes sombres).
* Utiliser `Card`, `Row`, `Column`, `LazyColumn` (si défilement nécessaire) de Material3.
* S'assurer de la réactivité des états (State Hoisting) : lier les nouvelles métriques aux ViewModels / repositories existants dans l'application.

---

#### 3. Actions attendues
1. Mettre à jour les modèles de données/ViewModels si nécessaire pour exposer :
   * `lastGmailSyncTimestamp`
   * `unqualifiedArticlesCount`
   * `pendingQualifiedArticlesCount`
   * `recentNotebooksList` (Top 3)
2. Adapter la vue Jetpack Compose du `DashboardScreen`.