# Stage Academy API

Cette API FastAPI permet de créer un compte, de se connecter et de gérer son
profil. La première version utilise SQLite et se lance en local.

## 1. Installer et démarrer sous Windows

Le projet utilise `uv` et Python 3.15.2. Si `uv` manque, installe-le avec cette
commande, puis rouvre PowerShell. [Guide d’installation de uv](https://docs.astral.sh/uv/getting-started/installation/#winget)

```powershell
winget install --id astral-sh.uv -e
```

Si `uv` ne trouve pas Python 3.15.2, installe cette version depuis le site
officiel de [Python pour Windows](https://www.python.org/downloads/windows/).
Depuis la racine du projet, crée ensuite l’environnement virtuel et installe
les dépendances.

```powershell
cd backend
uv venv --python 3.15.2
uv sync --locked
```

Crée ton fichier `.env` à partir du modèle. Cette commande génère une clé
aléatoire pour signer les jetons JWT et conserve le fichier s’il existe déjà.

```powershell
if (!(Test-Path -LiteralPath '.env')) {
    uv run --locked python -c "from pathlib import Path; import secrets; example = Path('.env.example').read_text(encoding='utf-8'); Path('.env').open('x', encoding='utf-8').write(example.replace('JWT_SECRET_KEY=', 'JWT_SECRET_KEY=' + secrets.token_urlsafe(48)))"
}
```

Applique la migration. Alembic crée le fichier SQLite `local.db` et sa table
`users`.

```powershell
uv run --locked alembic upgrade head
```

Démarre Uvicorn et ouvre [Swagger](http://127.0.0.1:8000/docs).

```powershell
uv run --locked uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

## 2. Tester l’API dans Swagger

1. Ouvre **POST /auth/register** et clique sur **Try it out**. Colle ce JSON et
   clique sur **Execute**. Le code **201** indique que le compte a été créé.

   ```json
   {
     "username": "camille",
     "email": "camille@example.com",
     "password": "Exemple-de-mot-de-passe-123"
   }
   ```

2. Ouvre **POST /auth/login** et clique sur **Try it out**. Envoie le même
   `username` et le même `password` dans le JSON. Copie la valeur
   d’`access_token` dans la réponse **200**.
3. Clique sur **Authorize** en haut de la page et colle le jeton seul dans
   **Value**. Swagger ajoutera le préfixe `Bearer` dans l’en-tête
   `Authorization`. Le jeton expire au bout de 30 minutes par défaut.
4. Lance **GET /users/me** pour voir le profil. Lance-le aussi après **Logout**
   dans Swagger pour observer une réponse **401**.

Pour tester une nouvelle inscription, utilise un autre nom et une autre
adresse. Un doublon donne une réponse **409**.

## 3. Routes disponibles

| Méthode et route | Action | Succès |
| --- | --- | --- |
| `POST /auth/register` | Créer un compte | 201 |
| `POST /auth/login` | Obtenir un JWT avec le nom et le mot de passe | 200 |
| `GET /users/me` | Lire le profil connecté | 200 |
| `PUT /users/me` | Modifier le nom et l’adresse e-mail ensemble | 200 |
| `PATCH /users/me` | Modifier un ou plusieurs champs du profil | 200 |
| `DELETE /users/me` | Supprimer le compte connecté | 204, corps vide |
| `GET /users?offset=0&limit=20` | Lister les comptes en tant qu’administrateur | 200 |

Pour tester le rôle administrateur, inscris le compte puis lance cette commande
dans un deuxième terminal, depuis `backend/`.

```powershell
uv run --locked python -m app.admin camille
```

L’API lit le rôle dans la base à chaque requête privée. L’inscription et
l’édition du profil donnent accès uniquement aux champs prévus.

## 4. Où se trouve le code

| Fichier ou dossier | Rôle |
| --- | --- |
| `app/main.py` | Crée l’API, monte les routes et prépare les réponses d’erreur. |
| `app/core/config.py` | Lit l’URL de la base, la clé JWT et la durée des jetons. |
| `app/core/security.py` | Hache les mots de passe avec Argon2id et signe et vérifie les JWT. |
| `app/database.py` | Crée le moteur SQLAlchemy et fournit une session par requête. |
| `app/models/user.py` | Décrit la table des utilisateurs et ses contraintes d’unicité. |
| `app/schemas/auth.py`, `app/schemas/user.py` | Définissent les données reçues et les champs retournés. |
| `app/routers/auth.py`, `app/routers/users.py` | Définissent les URL, les méthodes et les codes HTTP. |
| `app/services/users.py` | Contient les opérations sur les comptes et les accès à la base. |
| `app/dependencies.py` | Identifie l’utilisateur connecté et contrôle le rôle administrateur. |
| `app/admin.py` | Permet de promouvoir un compte depuis le terminal du serveur. |
| `alembic.ini`, `alembic/` | Configurent Alembic et conservent les migrations. |
| `tests/conftest.py`, `tests/test_api.py` | Préparent des bases isolées et testent l’API. |
| `pyproject.toml`, `uv.lock`, `.python-version` | Déclarent les dépendances, figent leurs versions et choisissent Python. |
| `.env.example`, `.gitignore` | Donnent le modèle de configuration locale et gardent les secrets et fichiers générés hors de Git. |

Quand tu t’inscris, Pydantic vérifie les champs, le service transforme le mot
de passe en hachage Argon2id et SQLAlchemy enregistre le compte. Quand tu te
connectes, le service vérifie ce hachage et renvoie un jeton signé. Le schéma
de réponse du profil contient uniquement les champs publics du compte.

## 5. Tests et migrations

```powershell
uv run --locked python -m pytest
uv run --locked alembic check
```

Après une modification des modèles, génère une nouvelle migration, relis le
fichier généré et applique-la.

```powershell
uv run --locked alembic revision --autogenerate -m "description du changement"
uv run --locked alembic upgrade head
```

## 6. Passer plus tard à PostgreSQL

Crée une base PostgreSQL, active le pilote et remplace `DATABASE_URL` dans
`.env` par l’adresse de cette base.

```powershell
uv sync --locked --extra postgres
```

```dotenv
DATABASE_URL=postgresql+psycopg://utilisateur:mot_de_passe@localhost:5432/stage_academy
```

```powershell
uv run --locked --extra postgres alembic upgrade head
uv run --locked --extra postgres uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Alembic crée les tables dans PostgreSQL. Pour retrouver des comptes existants,
il faut aussi importer les lignes de la base SQLite.
