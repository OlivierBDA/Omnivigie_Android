import os
import json
import asyncio
import functions_framework
from flask import jsonify

# On s'assure que le dossier /tmp/notebooklm est configuré pour stocker les cookies de session
NOTEBOOKLM_DIR = "/tmp/notebooklm"
os.environ["NOTEBOOKLM_HOME"] = NOTEBOOKLM_DIR
os.makedirs(os.path.join(NOTEBOOKLM_DIR, "profiles", "default"), exist_ok=True)

from notebooklm.client import NotebookLMClient
from notebooklm.rpc import AudioLength, AudioFormat

def save_session_cookies(storage_state_dict):
    """
    Sauvegarde les cookies de session fournis dans le dossier /tmp/notebooklm
    pour que NotebookLMClient puisse s'authentifier.
    """
    profile_dir = os.path.join(NOTEBOOKLM_DIR, "profiles", "default")
    os.makedirs(profile_dir, exist_ok=True)
    
    storage_state_path = os.path.join(profile_dir, "storage_state.json")
    with open(storage_state_path, "w", encoding="utf-8") as f:
        json.dump(storage_state_dict, f, ensure_ascii=False, indent=2)

async def handle_create_notebook(notebook_name):
    async with await NotebookLMClient.from_storage() as client:
        nb = await client.notebooks.create(notebook_name)
        return {
            "notebook_id": nb.id,
            "notebook_name": nb.title,
            "status": "created"
        }

async def handle_add_sources(notebook_id, urls):
    async with await NotebookLMClient.from_storage() as client:
        added_urls = []
        errors = []
        for url in urls:
            try:
                await client.sources.add_url(notebook_id, url)
                added_urls.append(url)
            except Exception as e:
                errors.append({"url": url, "error": str(e)})
        
        return {
            "notebook_id": notebook_id,
            "added_urls": added_urls,
            "errors": errors,
            "status": "success" if not errors else "partial_success"
        }

async def handle_generate_podcast(notebook_id):
    async with await NotebookLMClient.from_storage() as client:
        instructions = (
            "Fais une analyse approfondie et détaillée de ces documents. "
            "Prends le temps d'expliquer les concepts clés, les enjeux technologiques et les impacts. "
            "Le ton doit être professionnel, captivant et analytique. "
            "L'audio est destiné à être écouté par des ingénieurs et des architectes data / IA."
        )
        status = await client.artifacts.generate_audio(
            notebook_id=notebook_id,
            language="fr",
            instructions=instructions,
            audio_length=AudioLength.LONG,
            audio_format=AudioFormat.DEEP_DIVE
        )
        return {
            "notebook_id": notebook_id,
            "task_id": status.task_id,
            "status": "generating"
        }

@functions_framework.http
def hello_http(request):
    """
    Point d'entrée de la Cloud Function.
    Gère les appels asynchrones NotebookLM via un paramètre 'action'.
    """
    # CORS headers
    if request.method == "OPTIONS":
        headers = {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "POST",
            "Access-Control-Allow-Headers": "Content-Type, Authorization",
            "Access-Control-Max-Age": "3600"
        }
        return ("", 240, headers)

    headers = {"Access-Control-Allow-Origin": "*"}

    try:
        request_json = request.get_json(silent=True)
        if not request_json:
            return (jsonify({"error": "Requête vide ou JSON invalide"}), 400, headers)

        action = request_json.get("action")
        storage_state = request_json.get("notebooklm_storage_state")

        if not action:
            return (jsonify({"error": "Le paramètre 'action' est obligatoire"}), 400, headers)
        
        if not storage_state:
            return (jsonify({"error": "Le paramètre 'notebooklm_storage_state' (cookies de session) est obligatoire"}), 400, headers)

        # Sauvegarder les cookies reçus pour ce traitement
        save_session_cookies(storage_state)

        # Sélection et exécution de l'action demandée
        if action == "create_notebook":
            notebook_name = request_json.get("notebook_name")
            if not notebook_name:
                return (jsonify({"error": "Le paramètre 'notebook_name' est requis pour cette action"}), 400, headers)
            
            result = asyncio.run(handle_create_notebook(notebook_name))
            return (jsonify(result), 200, headers)

        elif action == "add_sources":
            notebook_id = request_json.get("notebook_id")
            urls = request_json.get("urls")
            if not notebook_id or not urls or not isinstance(urls, list):
                return (jsonify({"error": "Les paramètres 'notebook_id' et 'urls' (liste) sont requis pour cette action"}), 400, headers)
            
            result = asyncio.run(handle_add_sources(notebook_id, urls))
            return (jsonify(result), 200, headers)

        elif action == "generate_podcast":
            notebook_id = request_json.get("notebook_id")
            if not notebook_id:
                return (jsonify({"error": "Le paramètre 'notebook_id' est requis pour cette action"}), 400, headers)
            
            result = asyncio.run(handle_generate_podcast(notebook_id))
            return (jsonify(result), 200, headers)

        else:
            return (jsonify({"error": f"Action '{action}' non reconnue"}), 400, headers)

    except Exception as e:
        return (jsonify({"error": str(e)}), 500, headers)
