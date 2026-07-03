# Omnivigie Android (L'Assistant Veille sur Mobile)

Omnivigie Android est la migration mobile de l'assistant de veille technologique automatisé Omnivigie (initialement en Python). Cette application native Android (Kotlin/Jetpack Compose) est optimisée pour s'exécuter directement sur un Google Pixel 10 Pro.

L'objectif est de récupérer les newsletters de veille (TLDR) depuis Gmail, d'extraire les articles, d'évaluer leur pertinence via Gemini (qualification automatique), puis de générer des podcasts d'analyse via Google NotebookLM.

---

## 🏗️ Architecture Technique

L'application respecte les principes de la **Clean Architecture** et du pattern **MVVM** :
*   **Couche UI** : Jetpack Compose avec un thème "Cosmic Dark" (Palette violette/indigo).
*   **Couche Data** : 
    *   **Room DB** : Stockage local des emails, articles et réglages.
    *   **Retrofit / OkHttp** : Client pour l'API Gmail.
    *   **Credential Manager** : Authentification Google "One Tap".
    *   **Jsoup** : Parsing HTML complexe pour l'extraction d'articles.

---

## 📊 État d'Avancement des Fonctionnalités

### 1. Fondations & Interface [Terminé]
*   Setup Gradle 9.4, AGP 9.2, SDK 37 (Android 15).
*   Thème cosmique sombre et navigation par onglets (Dashboard, Curation, Paramètres).

### 2. Persistance Locale (Room) [Terminé]
*   Entités `EmailEntity`, `ArticleEntity` et `SettingEntity`.
*   Support des `Flow` pour une UI réactive.

### 3. Acquisition Gmail [Terminé]
*   Authentification via **Credential Manager** et **AuthorizationClient** (OAuth 2.0).
*   Récupération filtrée par requête Gmail dynamique (paramétrable dans l'UI).
*   Extraction du corps HTML avec décodage Base64.

### 4. Extraction & Structuration (Jsoup) [Terminé]
*   Portage de la logique Python pour découper les emails en articles structurés.
*   Nettoyage automatique des URLs (suppression des UTM/tracking).
*   Détection des sponsors et temps de lecture.
*   Filtres d'exclusion de mots-clés (ex: "Apply here").

### 5. Qualification IA (Gemini) [Terminé]
*   Intégration du SDK Google AI (Gemini 2.0 Flash Lite).
*   Pré-filtrage intelligent : détection automatique des publicités (N/A) et articles courts (< 5 min).
*   Prompt structuré pour l'analyse de pertinence, l'attribution de thèmes et la génération d'explications.
*   Système de curation visuel avec indicateurs de statut et nettoyage automatique.

---

## 🛠️ Éléments Techniques Structurants (Pour future session)

### Configuration Requise
*   **local.properties** : Doit contenir `WEB_CLIENT_ID` (OAuth Web) et `GEMINI_API_KEY`.
*   **Google Cloud Console** : L'app nécessite un Client ID **Android** (lié au SHA-1 de debug) ET un Client ID **Web**.
*   **Scopes OAuth** : `https://www.googleapis.com/auth/gmail.readonly` doit être autorisé.

### Logique de Parsing (ArticleExtractor.kt)
Le parsing est basé sur la structure des newsletters TLDR :
- Itération sur les blocs `div.text-block`.
- Identification du titre via `a > strong`.
- Résumé extrait depuis les `span` ayant un style `font-family`.
- Nettoyage des paramètres d'URL via une logique manuelle pour éviter les dépendances Android dans les tests unitaires.

### Flux de Données
1. `HomeViewModel` déclenche la sync.
2. `AuthManager` gère le jeton (et la résolution de consentement si besoin via `MainActivity`).
3. `GmailRepository` télécharge l'email -> sauvegarde dans Room -> déclenche `ArticleExtractor` -> sauvegarde les articles dans Room.
4. L'UI (Dashboard/Curation) observe les tables via Flow et se met à jour automatiquement.
