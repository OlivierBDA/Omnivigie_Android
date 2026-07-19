import os
import sys
import json
import requests
import subprocess
import shutil

# Configurer l'encodage UTF-8 pour la console sous Windows afin d'éviter les erreurs Unicode avec les émojis
if sys.platform.startswith("win"):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

# URL de votre Google Cloud Function (à adapter si vous testez localement avec functions-framework)
# Pour tester en local : URL = "http://localhost:8080"
URL = "https://omnivigie-python-backend-306370227717.europe-west1.run.app"

# Nom du carnet et URLs fictives demandées par l'utilisateur
NOTEBOOK_NAME = "[AI] 2026-07-19 MCP et A2A"
TEST_URLS = [
    "https://www.anthropic.com/news/model-context-protocol",
    "https://developers.googleblog.com/en/a2a-a-new-era-of-agent-interoperability/"
]

def get_google_id_token():
    """
    Récupère le jeton d'authentification IAM Google Cloud via gcloud.
    Nécessaire pour passer à travers l'authentification IAM de votre Cloud Function.
    """
    try:
        result = subprocess.run(
            ["gcloud", "auth", "print-identity-token"],
            capture_output=True,
            text=True,
            check=True,
            shell=True
        )
        token = result.stdout.strip()
        if token:
            return token
    except Exception as e:
        print(f"⚠️ Échec de la récupération du jeton d'identité Google Cloud : {e}")
        print("Assurez-vous que gcloud est installé et connecté ('gcloud auth login').")
    return None

def load_notebooklm_cookies():
    """
    Lit le fichier storage_state.json généré localement par la commande 'notebooklm login'.
    """
    cookie_path = os.path.expanduser("~/.notebooklm/profiles/default/storage_state.json")
    
    if not os.path.exists(cookie_path):
        print(f"❌ Erreur : Fichier de session NotebookLM introuvable à l'emplacement : {cookie_path}")
        print("Veuillez d'abord vous connecter à NotebookLM localement en exécutant :")
        print("   python -m notebooklm login")
        return None

    try:
        with open(cookie_path, "r", encoding="utf-8") as f:
            cookies_dict = json.load(f)
            return cookies_dict
    except Exception as e:
        print(f"❌ Erreur lors de la lecture du fichier de cookies : {e}")
        return None

def call_backend(payload, headers):
    """
    Appelle la Cloud Function avec le payload JSON fourni.
    """
    try:
        response = requests.post(URL, json=payload, headers=headers)
        if response.status_code == 200:
            return response.json()
        else:
            print(f"❌ Échec de l'appel backend (Statut {response.status_code}) : {response.text}")
            return None
    except Exception as e:
        print(f"❌ Erreur de connexion avec la Cloud Function : {e}")
        return None

def run_test_flow():
    print("==================================================")
    print(" DÉBUT DU TEST DU PIPELINE NOTEBOOKLM SUR GCP")
    print("==================================================\n")

    # 1. Authentification Google Cloud (IAM)
    print("🔑 Étape 1/2 : Récupération du jeton IAM Google Cloud...")
    gcp_token = get_google_id_token()
    if not gcp_token:
        print("❌ Impossible de continuer sans jeton Google Cloud.")
        return

    headers = {
        "Authorization": f"Bearer {gcp_token}",
        "Content-Type": "application/json",
    }

    # 2. Récupération des cookies de session NotebookLM
    print("🍪 Étape 2/2 : Lecture de la session locale NotebookLM...")
    notebooklm_cookies = load_notebooklm_cookies()
    if not notebooklm_cookies:
        return

    print("✅ Authentification prête. Démarrage des actions séquentielles sur le Cloud backend...\n")

    # ================= ACTION 1 : CRÉATION DU NOTEBOOK =================
    print("📂 [Action 1/3] Création du carnet dans NotebookLM...")
    create_payload = {
        "action": "create_notebook",
        "notebook_name": NOTEBOOK_NAME,
        "notebooklm_storage_state": notebooklm_cookies
    }
    
    result = call_backend(create_payload, headers)
    if not result:
        print("❌ Échec de la création du carnet. Arrêt du flux.")
        return

    notebook_id = result.get("notebook_id")
    notebook_name_res = result.get("notebook_name")
    print(f"🎉 Carnet créé avec succès ! ID: {notebook_id} | Nom: {notebook_name_res}\n")

    # ================= ACTION 2 : AJOUT DES SOURCES =================
    print("🔗 [Action 2/3] Ajout des articles de test au carnet...")
    add_payload = {
        "action": "add_sources",
        "notebook_id": notebook_id,
        "urls": TEST_URLS,
        "notebooklm_storage_state": notebooklm_cookies
    }

    result = call_backend(add_payload, headers)
    if not result:
        print("❌ Échec de l'ajout des sources. Arrêt du flux.")
        return

    added_urls = result.get("added_urls", [])
    errors = result.get("errors", [])
    
    print(f"✅ Ajout terminé ! ({len(added_urls)} URLs ajoutées avec succès)")
    for url in added_urls:
        print(f"   - [OK] {url}")
    for err in errors:
        print(f"   - [ERREUR] {err.get('url')} : {err.get('error')}")
    print()

    # ================= ACTION 3 : GÉNÉRATION DU PODCAST =================
    print("🎙️ [Action 3/3] Lancement de la génération du podcast long (fr)...")
    podcast_payload = {
        "action": "generate_podcast",
        "notebook_id": notebook_id,
        "notebooklm_storage_state": notebooklm_cookies
    }

    result = call_backend(podcast_payload, headers)
    if not result:
        print("❌ Échec du lancement de la génération du podcast.")
        return

    task_id = result.get("task_id")
    print(f"🚀 Génération du podcast lancée avec succès ! Task ID: {task_id}")
    print("L'opération est longue (5 à 10 minutes).")
    print("Vous pouvez suivre sa progression directement sur l'interface web de NotebookLM.")
    print("\n==================================================")
    print(" TEST DU FLUX TERMINÉ AVEC SUCCÈS")
    print("==================================================")

if __name__ == "__main__":
    run_test_flow()
