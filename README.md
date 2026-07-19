# Omnivigie Android (L'Assistant Veille sur Mobile)

Omnivigie Android est la migration mobile de l'assistant de veille technologique automatisé Omnivigie (initialement en Python). Cette application native Android (Kotlin/Jetpack Compose) est optimisée pour s'exécuter directement sur un Google Pixel 10 Pro.

L'objectif est de récupérer les newsletters de veille (TLDR) depuis Gmail, d'extraire les articles, d'évaluer leur pertinence via Gemini (qualification automatique), puis de générer des podcasts d'analyse via Google NotebookLM.

---

## 🏗️ Architecture Technique

L'application respecte les principes de la **Clean Architecture** et du pattern **MVVM** :
*   **Couche UI** : Jetpack Compose avec un thème "Cosmic Dark" (Palette violette/indigo).
*   **Couche Data** : 
    *   **Room DB** : Stockage local des emails, articles et réglages.
    *   **Retrofit / OkHttp** : Client pour l'API Gmail et le backend GCP.
    *   **Credential Manager** : Authentification Google "One Tap" et jetons IAM.
    *   **Jsoup** : Parsing HTML complexe pour l'extraction d'articles.
*   **Backend Hybrid** : Utilisation d'une **Cloud Function GCP (Python)** pour stabiliser les interactions complexes avec NotebookLM.

---

## 📊 État d'Avancement des Fonctionnalités

### 1. Fondations & Interface [Terminé]
*   Setup Gradle 9.4, AGP 9.2, SDK 37 (Android 15).
*   Thème cosmique sombre et navigation par onglets.

### 2. Persistance Locale (Room) [Terminé]
*   Entités `EmailEntity`, `ArticleEntity` et `SettingEntity`.
*   Gestion réactive via `Flow`.

### 3. Acquisition Gmail [Terminé]
*   Authentification via **Credential Manager** et **AuthorizationClient**.
*   Récupération filtrée par requête Gmail dynamique.

### 4. Extraction & Structuration (Jsoup) [Terminé]
*   Nettoyage des URLs (suppression UTM/tracking).
*   Détection des sponsors et temps de lecture.

### 5. Qualification IA (Gemini) [Terminé]
*   Intégration du SDK Google AI (Gemini 2.0 Flash Lite).
*   Pré-filtrage intelligent (Sponsors, articles < 5 min, publicités N/A).
*   Système de curation visuel avec indicateurs de statut.

### 6. Authentification NotebookLM [Terminé]
*   Interface WebView pour la connexion sécurisée.
*   Capture des sessions au format **Playwright (storage_state.json)** pour compatibilité backend.
*   Stockage chiffré via `EncryptedSharedPreferences`.

### 7. Création de Carnets & Ajout de Sources [Terminé]
*   Flux de curation thématique : sélection manuelle et suppression d'articles.
*   **Backend GCP** : Délégation de la création de notebooks à une Cloud Function Python sécurisée par IAM.
*   Envoi groupé (Batch) des sources pour une performance optimale.

### 8. Génération de Podcast Audio [Terminé]
*   Automatisation de la synthèse audio "Deep Dive" dans NotebookLM.
*   Pipeline complet avec attente intelligente d'indexation (30s) intégrée.

---

## 🛠️ Évolutions Clés : Architecture GCP

Le projet a évolué vers une architecture hybride pour garantir la fiabilité des interactions avec NotebookLM :
- **Sécurité IAM** : L'app Android récupère un jeton d'identité Google (ID Token) pour appeler de manière sécurisée les services GCP.
- **Délégation Python** : Les opérations lourdes de NotebookLM (API RPC interne) sont exécutées par une Cloud Function Python, réutilisant la robustesse de la librairie `notebooklm-py`.
- **Format de Session** : Passage d'une simple chaîne de cookies à un état de stockage Playwright complet pour une gestion de session transparente entre le mobile et le backend.

### Flux de Données Final
1. `HomeViewModel` -> `AuthManager` (ID Token GCP).
2. `HomeViewModel` -> `CreateThemedNotebookUseCase` (Pipeline).
3. `CreateThemedNotebookUseCase` -> `NotebookLmRepository` -> `GCP Cloud Function`.
4. La Cloud Function pilote NotebookLM et renvoie les IDs de création.
5. Mise à jour finale de la base Room locale.
