# Omnivigie Android (L'Assistant Veille sur Mobile)

Omnivigie Android est la migration mobile de l'assistant de veille technologique automatisé Omnivigie (initialement développé en Python). Cette application native Android (Kotlin/Jetpack Compose) est optimisée pour s'exécuter directement sur un Google Pixel 10 Pro.

L'objectif de l'application est de récupérer vos newsletters de veille directement depuis votre boîte Gmail, d'en extraire les articles, d'évaluer leur pertinence sémantique via l'IA de Google Gemini (qualification automatique), puis de créer des carnets thématiques et de générer des podcasts d'analyse (Deep Dive) directement dans Google NotebookLM.

---

## 🏗️ Architecture Technique

L'application respecte les principes de la **Clean Architecture** avec une séparation en couches et un pattern **MVVM** (Model-View-ViewModel) :
*   **Couche UI** : Jetpack Compose avec un thème cosmique sombre premium (palette violette et indigo vibrante, typographies modernes).
*   **Couche Domain** : Contient la logique métier, les modèles de données de veille et les use cases.
*   **Couche Data** : 
    *   **Room DB** : Base de données locale pour persister les emails, les articles extraits, et les réglages utilisateur.
    *   **Retrofit / OkHttp** : Client de communication HTTP pour Gmail API et l'API RPC de NotebookLM.
    *   **Google AI Client SDK** : Appels directs à l'API Gemini pour la qualification IA.
    *   **WorkManager** : Tâches planifiées en arrière-plan pour automatiser le pipeline de veille chaque matin.

---

## 📊 État d'Avancement des Fonctionnalités

### 1. Fondations & Interface Visuelle
*   **Initialisation du projet & Setup Gradle** : **[Terminé]**
    *   Configuration Kotlin DSL, Gradle 8.2, SDK cible 34.
    *   Dépendances configurées (Compose, Room, Retrofit, Gemini, Jsoup, WorkManager, Google Auth).
*   **Thème sombre cosmique** : **[Terminé]**
    *   Définition de la palette de couleurs, des styles typographiques et de la gestion de la barre de statut.
*   **Squelette de l'Interface utilisateur (Jetpack Compose)** : **[Terminé]**
    *   Navigation par onglets (Dashboard, Curation, Paramètres) avec listes et widgets factices.

### 2. Persistance Locale
*   **Schéma et Entités Room (Email, Article)** : **[En cours]**
    *   Modélisation des tables `emails` et `articles`.
*   **DAOs & Base de données locale** : **[À faire]**
    *   Implémentation des requêtes de persistance, de filtrage par thème et de mise à jour des statuts.

### 3. Pipeline d'Acquisition & Extraction
*   **Connexion OAuth 2.0 Gmail** : **[À faire]**
    *   Intégration du Google Sign-In / Credential Manager et des permissions Gmail Readonly.
*   **Récupération des emails (Gmail API)** : **[À faire]**
    *   Requête de recherche des emails TLDR non encore téléchargés.
*   **Parser HTML (Jsoup)** : **[À faire]**
    *   Extraction des métadonnées d'articles (titres, liens nettoyés sans tracking, temps de lecture, résumés, détection des sponsors).

### 4. Qualification Sémantique par l'IA
*   **Intégration du SDK Google AI** : **[À faire]**
    *   Appels à Gemini Flash avec gestion sécurisée de la clé d'API.
*   **Filtre Métier & Prompting Structuré** : **[À faire]**
    *   Exclusion automatique des sponsors et articles courts, et qualification des articles restants sous format JSON structuré avec tags issus de `themes.json` et critères de `criteria.md`.

### 5. Intégration Google NotebookLM
*   **Authentification par WebView** : **[À faire]**
    *   Capture des cookies de session Google et extraction automatique du token CSRF (`SNlM0e`) et du Session ID (`FdrFJe`).
*   **Client API RPC (Retrofit)** : **[À faire]**
    *   Création de carnet (`CREATE_NOTEBOOK`) et ajout de sources (`ADD_SOURCE`) via requêtes HTTP `batchexecute`.
*   **Génération de Podcast Audio** : **[À faire]**
    *   Déclenchement du Deep Dive Audio avec instructions personnalisées.

### 6. Automatisation & Notifications
*   **Planificateur d'arrière-plan (WorkManager)** : **[À faire]**
    *   Exécution nocturne ou quotidienne automatique du pipeline complet.
*   **Notifications Système** : **[À faire]**
    *   Alerte l'utilisateur sur son Pixel 10 Pro lorsque de nouveaux articles pertinents ont été qualifiés.
