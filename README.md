# Omnivigie Android - L'Assistant de Veille Technologique Automatisé

**Omnivigie Android** est une application native Android (Kotlin / Jetpack Compose) de veille technologique automatisée. Elle transforme la lecture passive de newsletters IT (ex: *TLDR AI*, *TLDR Tech*) reçues sur Gmail en une chaîne d'analyse intelligente alimentée par **Google Gemini 2.0 Flash Lite** et **Google Gemini Notebook (ex-NotebookLM)**.

---

## 🎯 Pipeline Fonctionnel Détaillé

Omnivigie orchestre l'ensemble du pipeline de veille technologique en 5 grandes étapes :

1. **Acquisition Gmail (Filtre par Date)** :
   - Connexion sécurisée à votre compte Gmail via l'API Google OAuth2 (Credential Manager).
   - Récupération ciblée des newsletters via une requête dynamique `from:dan@tldrnewsletter.com OR from:tldr@tldrnewsletter.com after:YYYY/MM/DD`.
   - La date de début est sélectionnable visuellement via un **Calendrier interactif (Material3 DatePicker)** dans les Paramètres.

2. **Parsing HTML & Extraction des Articles (Jsoup)** :
   - Parsing HTML haute précision pour isoler chaque article (titre, URL, résumé, temps de lecture, détection des sponsors).
   - Nettoyage automatique des URLs (suppression des paramètres de tracking UTM).

3. **Qualification par IA & Rétention des Articles (On-Device AICore TPU & Cloud API)** :
   - **Niveau 0 (Rétention & Expiration)** : Dès le lancement de l'action *"Sync & Traiter la veille"*, les articles dont la date de réception est plus ancienne que le seuil de rétention configuré (`articleRetentionDays`, 30 jours par défaut dans l'onglet Paramètres) sont immédiatement basculés dans la catégorie **"Exclus"** sans consommer de quota LLM.
   - **Niveau 1 (Pré-filtrage)** : Les articles ayant un temps de lecture inférieur au seuil (`minReadingTime`), marqués "N/A" ou sponsors sont immédiatement qualifiés avec `aiInterest = false` et basculés dans la catégorie **"Exclus"**.
   - **Niveau 2 (Classification Hybride On-Device / Cloud)** :
     - **Mode Local par défaut (Android AICore / ML Kit GenAI Prompt API)** : Inférence exécutée directement sur le TPU Tensor d'un Pixel 10 Pro avec le modèle **Gemini Nano 4 Fast** (E2B / faible latence, température déterministe `0.1`). Confidentialité totale, zéro latence réseau et aucune consommation de quota d'API.
     - **Fallback Automatique & Sélecteur de Moteur** : Si le modèle AICore n'est pas encore téléchargé ou indisponible, l'application bascule automatiquement sur l'API distante **Google Gemini 2.0 Flash Lite**. Un commutateur dans les Paramètres permet également de forcer l'un ou l'autre des moteurs à tout moment.
   - **Gestion des Quotas API LLM (`QuotaExceededException` / Error 429)** : En cas d'utilisation de l'API Cloud (ou de fallback) et si le quota d'appel API Gemini est dépassé, l'erreur est interceptée, la qualification est **interrompue immédiatement** et un message d'alerte *"Qualification interrompue (quota API LLM atteint)"* s'affiche sur le Dashboard. Les articles non encore analysés restent en **"Non classé"** pour être repris lors du prochain traitement.

4. **Curation & Gestion des Catégories** :
   - **"Non classé"** (en haut de liste) : Articles fraîchement récupérés en attente de qualification.
   - **Thèmes qualifiés** (*IA Générative & LLM*, *Data Engineering*, *Agents Autonomes*...) : Articles approuvés (`aiInterest = true`).
   - **"Exclus"** (en bas de liste) : Articles rejetés par le Niveau 1 ou le Niveau 2.
   - **Purge ciblée (Poubelle Rouge)** : Un clic sur la poubelle rouge en haut à droite de l'écran Curation supprime **exclusivement** tous les articles de la catégorie **"Exclus"** (avec confirmation). Les articles en "Non classé" restent intacts.

5. **Génération Gemini Notebook & Podcast Audio** :
   - **Recommandation d'articles par IA (Focus Thème)** : Dans l'écran de détail d'un thème, un clic sur *"Suggestion d'articles"* envoie les candidats du thème à Gemini 2.0 Flash Lite. L'IA sélectionne entre 5 et 8 articles partageant un fil conducteur commun et justifie son choix. En acceptant, les articles suggérés sont automatiquement cochés et les autres décochés.
   - **Création du Notebook** : Clic sur *"Création du Notebook"* ➔ Appel sécurisé du backend GCP (Cloud Function Python `gcp_backend/`).
   - Création du carnet dans Gemini Notebook (`https://notebook.google.com/`), ajout en lot des URLs sources, attente d'indexation (30s) et lancement de la génération du **Podcast Audio "Deep Dive"** en français.
   - Clic sur un carnet récent dans le Dashboard ➔ Ouverture directe de l'application officielle **Google Gemini Notebook** sur le smartphone via deep-linking (`https://notebook.google.com/notebook/<ID>`).


---

## 📱 Galerie des Écrans (Captures d'Écran)

| Dashboard Omnivigie | Écran de Curation |
| :---: | :---: |
| ![Tableau de Bord](Omnivigie_Dashboard.png) | ![Curation par Thème](Omnivigie_Curation.png) |
| **Tour de contrôle, diagnostic système, synchro et carnets récents** | **Curation par thèmes, articles "Non classé" et "Exclus"** |

| Focus sur un Thème | Paramètres de Veille |
| :---: | :---: |
| ![Focus sur un Thème](Omnivigie_FocusTheme.png) | ![Paramètres de Veille](Omnivigie_Parametres.png) |
| **Détail d'un thème, sélection d'articles et création de Notebook** | **Filtre calendrier Gmail, critères criteria.md, thèmes et purge** |

---

## 🏗️ Architecture Technique & Migration Domaine

### 1. Prise en Charge de la Migration Domaine Google (Gemini Notebook)
Google a migré le service sous le nom **Gemini Notebook** et la nouvelle URL racine **`https://notebook.google.com/`** :
- **`NotebookAuthActivity.kt`** : Détecte l'URL `notebook.google.com`, extrait les cookies de session (`SID`, `OSID`, etc.), construit la structure Playwright avec l'origine `https://notebook.google.com`, sauvegarde l'état de session dans `EncryptedSharedPreferences`, puis se ferme automatiquement (`finish()`).
- **`DashboardScreen.kt`** : Génère les deep-links `https://notebook.google.com/notebook/<ID>` pour ouvrir l'application officielle sur Android.

### 2. Backend Hybride GCP (`gcp_backend/`)
Le dossier `gcp_backend/` contient le code Python déployé sous forme de **Google Cloud Function HTTP** (Python 3.11+, `functions-framework`, `notebooklm-py>=0.4.0`) :
- **Authentification IAM** : Exige un jeton d'identité Google ID Token transmis par l'application Android (`Authorization: Bearer <ID_TOKEN>`).
- **Gestionnaire de Session (`main.py`)** : Reçoit `notebooklm_storage_state` et formule automatiquement les cookies et l'origine Playwright `/tmp/notebooklm/profiles/default/storage_state.json` exclusivement pour `https://notebook.google.com/`.
- **Actions supportées** :
  - `action = "create_notebook"` : Crée un carnet titré `[AI] YYYY-MM-DD TLDR-<Thème>`.
  - `action = "add_urls_batch"` : Ajoute les URLs d'articles en lot.
  - `action = "generate_podcast"` : Lance la synthèse audio "Deep Dive" (`AudioLength.LONG`, `AudioFormat.DEEP_DIVE`, langue `fr`).

### 3. Écran de Traitement Immersif (Vidéo & HUD Glassmorphic)
- **`ProcessingVideoScreen.kt`** : Lors des temps de traitement (synchronisation Gmail, qualification Gemini, création de carnets NotebookLM), une vidéo d'animation est lue en boucle en arrière-plan plein écran (via **AndroidX Media3 ExoPlayer**). Un bandeau d'interface translucide (*Glassmorphism*) affiche en sur-impression le statut en temps réel avec un bouton de fermeture ou de validation en fin d'opération.

### 4. Configuration Avancée du LLM (Paramètres)
- **Personnalisation du Modèle & Clé API** : L'écran Paramètres permet de configurer dynamiquement la clé API Google Gemini et le modèle à utiliser (avec lien direct vers la documentation officielle des modèles et bouton de test de connexion immédiat).

---

## 📂 Arborescence du Projet

```text
Omnivigie_Android/
├── README.md                              # Documentation officielle du projet
├── Omnivigie_Dashboard.png                # Capture d'écran du Tableau de bord
├── Omnivigie_Curation.png                 # Capture d'écran de la Curation par Thème
├── Omnivigie_FocusTheme.png               # Capture d'écran du Détail d'un Thème
├── Omnivigie_Parametres.png               # Capture d'écran de l'écran Paramètres
├── gcp_backend/                           # Code Python de la Cloud Function GCP
│   ├── main.py                            # Handler HTTP & adaptateur de session Multi-domaines
│   └── requirements.txt                   # Dépendances Python (notebooklm-py>=0.4.0...)
└── app/
    └── src/main/java/com/olivierbda/omnivigie/
        ├── app/                           # Main Application class & Database Module
        ├── data/
        │   ├── auth/                      # AuthManager (OAuth2, IAM) & SessionManager (EncryptedSharedPref)
        │   ├── extraction/                # ArticleExtractor (Jsoup)
        │   ├── local/                     # Room Database, DAOs & Entities
        │   ├── remote/                    # Retrofit GcpFunctionApiService
        │   └── repository/                # GmailRepository, GeminiRepository, NotebookLmRepository
        ├── domain/usecase/                # QualifyArticlesUseCase, CreateThemedNotebookUseCase, RecommendArticlesUseCase
        └── ui/
            ├── auth/                      # NotebookAuthActivity (WebView Multi-Domain Session Capture)
            ├── theme/                     # Palette Cosmic Dark & Composables de style
            ├── viewmodel/                 # HomeViewModel & NotebookSummary
            ├── DashboardScreen.kt         # Écran Dashboard
            ├── HomeScreen.kt              # Écran Principal (Tabs, Curation, Settings)
            ├── CurationDetailScreen.kt    # Écran de sélection des fiches par thème
            └── ProcessingVideoScreen.kt   # Écran immersif vidéo plein écran & HUD de progression
```

---

## 🛠️ Stack Technique

- **Langage & Framework** : Kotlin, Jetpack Compose, Coroutines, Flow, StateFlow.
- **Android SDK** : Compile SDK 37 (Android 15), Target SDK 35, Min SDK 26.
- **Multimédia & Vidéo** : AndroidX Media3 (ExoPlayer 1.5.1).
- **Base de Données Locale** : Room Database, EncryptedSharedPreferences.
- **IA & APIs** : Google AI SDK (Gemini 2.0 Flash Lite), Retrofit 2, OkHttp 4, Jsoup.
- **Backend Cloud** : GCP Cloud Function Python, `notebooklm-py` (>= 0.4.0), Google IAM Authentication.
- **Outils de Build** : Gradle 9.4, AGP 9.2, KSP.


---

## 📚 Documentation Technique Complémentaire

Pour une explication exhaustive et détaillée sur la gestion des cookies, l'authentification Google, le fonctionnement de la WebView et la résolution de toutes les anomalies rencontrées (ex: *"This browser or app may not be secured"*, `__Secure-1PSIDTS`, etc.), consultez le guide d'architecture :
- [DOCUMENTATION_NOTEBOOKLM_AUTH.md](file:///c:/Workplace/Dev/Omnivigie_Android/DOCUMENTATION_NOTEBOOKLM_AUTH.md)
