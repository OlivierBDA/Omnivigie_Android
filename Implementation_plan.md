# Plan d'Implémentation Complet : Migration Omnivigie vers Android

Ce document présente le plan d'implémentation complet pour porter l'application Omnivigie de Python vers un projet Android natif en Kotlin et Jetpack Compose. 

Le projet est développé étape par étape afin de faciliter les phases de tests et de débogage.

---

## 📅 Synthèse du Plan par Étapes

### Étape 1 : Initialisation du Projet & UI de Base [En cours]
*   **Objectif** : Initialiser la structure Gradle, intégrer les dépendances de base (Compose, Room, Retrofit, Gemini, Jsoup, WorkManager) et créer l'interface graphique d'accueil avec un thème sombre cosmique premium.
*   **Livrables** : 
    *   Fichiers Gradle configurés (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`).
    *   Structure de packages Clean Architecture (`data`, `domain`, `di`, `ui`).
    *   Composants de thématique Compose (`Color.kt`, `Type.kt`, `Theme.kt`).
    *   Dashboard interactif à 3 onglets (`HomeScreen.kt`, `MainActivity.kt`).
*   **Protocole de test** : Compilation et lancement sur l'émulateur Pixel 10 Pro ; vérification de la fluidité et du rendu visuel de la navigation.

---

### Étape 2 : Base de Données Locale (Room) & Entités
*   **Objectif** : Mettre en place le stockage local de l'application en migrant la base SQLite du script Python vers l'ORM Room d'Android.
*   **Actions** :
    *   Créer les entités `EmailEntity` (table `emails`) et `ArticleEntity` (table `articles`).
    *   Créer les interfaces DAO (`EmailDao`, `ArticleDao`) pour gérer les requêtes SQL (insertions, sélections par thèmes, filtrages).
    *   Créer la classe de base `OmnivigieDatabase`.
    *   Créer un écran ou bouton de débogage temporaire dans l'UI pour tester la persistance des données.
*   **Protocole de test** : Insérer des articles fictifs depuis l'UI, fermer l'application, la rouvrir et s'assurer que les articles sont toujours présents dans la liste.

---

### Étape 3 : Authentification & Récupération Gmail (Gmail API)
*   **Objectif** : Permettre à l'application de s'authentifier de manière sécurisée auprès de votre compte Google et de récupérer les newsletters de veille depuis votre boîte de réception Gmail.
*   **Actions** :
    *   Configurer le client OAuth Google dans la Google Cloud Console et l'enregistrer dans l'application via Credential Manager / Google Sign-In.
    *   Instancier le client API Gmail dans le projet Android.
    *   Implémenter la requête de recherche Gmail : `from:dan@tldrnewsletter.com OR from:tldr@tldrnewsletter.com after:{dernier_timestamp}`.
*   **Protocole de test** : Cliquer sur le bouton "Connexion Google", valider l'accès, puis cliquer sur "Synchroniser". Vérifier dans la console (Logcat) que les identifiants d'emails correspondants sont correctement reçus.

---

### Étape 4 : Extraction et Structuration des Articles (Parsing HTML)
*   **Objectif** : Extraire et nettoyer les articles contenus dans le corps HTML des emails téléchargés.
*   **Actions** :
    *   Intégrer la bibliothèque **Jsoup** pour le parsing HTML.
    *   Parcourir les éléments HTML pour extraire le titre, le temps de lecture, le résumé et marquer les articles de type Sponsor.
    *   Reconstruire la logique `clean_tldr_url` en Kotlin pour supprimer les redirections et les paramètres de tracking.
    *   Sauvegarder automatiquement les articles extraits dans la base de données Room.
*   **Protocole de test** : Lancer une synchronisation Gmail réelle, et vérifier que la liste de curation affiche correctement tous les articles (sans tracking d'URL et avec distinction des sponsors).

---

### Étape 5 : Qualification par l'IA (Google Gemini SDK)
*   **Objectif** : Évaluer automatiquement la pertinence technique des articles par rapport à vos centres d'intérêt.
*   **Actions** :
    *   Intégrer le SDK Android officiel de Google AI.
    *   Créer un panneau de configuration pour stocker la clé d'API Gemini, éditer vos critères de veille (`criteria.md`) et définir la liste des thèmes cibles (`themes.json`).
    *   Mettre en place le pré-filtrage métier (rejet automatique des sponsors et articles < 5 minutes).
    *   Envoyer les articles restants à Gemini en lui fournissant les critères et en lui imposant un format de réponse structuré (JSON) contenant l'intérêt (booléen), les thèmes assignés et l'explication.
    *   Enregistrer les décisions de l'IA en base de données.
*   **Protocole de test** : Lancer la qualification et vérifier que les articles exclus par le filtre métier sont marqués correctement, et que les articles analysés par Gemini reçoivent les explications et les tags de thématiques associés dans l'UI.

---

### Étape 6 : Authentification NotebookLM via WebView
*   **Objectif** : Capturer la session utilisateur nécessaire pour communiquer avec NotebookLM (qui n'a pas d'API publique).
*   **Actions** :
    *   Intégrer une WebView sécurisée pointant vers `https://notebooklm.google.com/` pour permettre à l'utilisateur de se connecter à son compte Google.
    *   Récupérer les cookies de session via `CookieManager`.
    *   Lancer une requête réseau GET d'initialisation en tâche de fond pour extraire le jeton CSRF (`SNlM0e`) et le Session ID (`FdrFJe`) du code HTML via des expressions régulières.
    *   Stocker ces jetons de session de manière sécurisée dans `EncryptedSharedPreferences`.
*   **Protocole de test** : Effectuer la connexion dans la WebView et s'assurer que le statut affiche "NotebookLM Connecté" avec récupération valide des jetons.

---

### Étape 7 : Création de Carnets & Ajout de Sources (NotebookLM API Client)
*   **Objectif** : Implémenter le client d'API RPC pour créer des carnets thématiques et y injecter les articles retenus.
*   **Actions** :
    *   Implémenter un client Retrofit ciblant l'endpoint `batchexecute` de NotebookLM.
    *   Mettre en place la sérialisation des payloads RPC pour les actions `CREATE_NOTEBOOK` (code `"CCqFvf"`) et `ADD_SOURCE` (code `"izAoDd"`).
    *   Dans l'onglet Curation, permettre à l'utilisateur de sélectionner des articles par thème et de lancer la création du carnet nommé `[AI] AAAA-MM-JJ TLDR-{Thème}`.
*   **Protocole de test** : Lancer la création d'un carnet pour un thème donné, puis ouvrir NotebookLM sur un navigateur PC pour vérifier que le carnet existe et contient bien les URLs sélectionnées.

---

### Étape 8 : Génération de Podcast Audio
*   **Objectif** : Automatiser le lancement de la synthèse audio de type "Deep Dive" dans NotebookLM.
*   **Actions** :
    *   Coder la requête RPC `CREATE_ARTIFACT` (code `"R7cb6c"`) avec les configurations audio (format `DEEP_DIVE`, langue `"fr"` et instructions personnalisées de veille).
    *   Ajouter un bouton d'action dans l'application pour déclencher cette génération une fois le carnet créé.
*   **Protocole de test** : Cliquer sur le bouton et vérifier dans le navigateur web que NotebookLM débute la génération audio du Deep Dive.

---

### Étape 9 : Pipeline Automatique & Interface Premium Polie
*   **Objectif** : Automatiser l'ensemble du pipeline et peaufiner l'expérience utilisateur.
*   **Actions** :
    *   Définir un `Worker` avec **WorkManager** pour exécuter la récupération, le parsing et la qualification automatiquement pendant la nuit.
    *   Ajouter des notifications push pour avertir l'utilisateur lorsque des articles pertinents ont été détectés.
    *   Polir l'interface graphique avec des micro-animations Compose, des dégradés subtils et des transitions fluides.
*   **Protocole de test** : Activer la tâche planifiée, s'assurer que le pipeline s'exécute correctement en tâche de fond et que la notification est bien reçue.
