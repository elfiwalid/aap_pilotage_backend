# Tests Selenium E2E Staff2Staff

Cette suite valide les parcours principaux via Microsoft Edge.

## Prerequis

- Backend Spring Boot lance sur `http://localhost:8080`
- Frontend React/Vite lance sur `http://localhost:5173`
- Base PostgreSQL locale disponible avec les comptes de demonstration
- Microsoft Edge installe
- `msedgedriver.exe` local compatible avec la version Edge du poste

Les tests ne demarrent pas automatiquement le backend ni le frontend.

## Installation EdgeDriver sur poste Axway/Sopra

Sur les PC professionnels, l'acces a `msedgedriver.azureedge.net` peut etre bloque par DNS/proxy. Les tests ne telechargent donc pas EdgeDriver automatiquement.

1. Telecharger manuellement Microsoft EdgeDriver compatible avec Edge `149.0.4022.69`.
2. Placer le binaire ici :

```text
C:\Tools\msedgedriver.exe
```

3. Lancer les tests Selenium avec le chemin explicite :

```bash
.\mvnw.cmd test -Dtest=*Selenium* -Dwebdriver.edge.driver="C:\Tools\msedgedriver.exe"
```

Le chemin peut aussi etre fourni via la variable d'environnement `MSEDGEDRIVER_PATH`.

## Commandes

Tests JUnit backend classiques :

```bash
.\mvnw.cmd test
```

Tests Selenium uniquement :

```bash
.\mvnw.cmd test -Dtest=*Selenium* -Dwebdriver.edge.driver="C:\Tools\msedgedriver.exe"
```

Mode navigateur visible :

```bash
.\mvnw.cmd test -Dtest=*Selenium* -Dwebdriver.edge.driver="C:\Tools\msedgedriver.exe" -Dselenium.headless=false
```

URL frontend personnalisee :

```bash
.\mvnw.cmd test -Dtest=*Selenium* -Dwebdriver.edge.driver="C:\Tools\msedgedriver.exe" -Ds2s.frontend.url=http://localhost:5173
```

Si `-Dwebdriver.edge.driver` est absent ou pointe vers un fichier inexistant, les tests Selenium sont ignores proprement avec un message explicite.

## Scenarios couverts

- Connexion Resource Manager et acces dashboard RM
- Connexion Chef de Projet et acces dashboard PM
- Connexion Collaborateur et acces dashboard collaborateur
- Navigation RM vers Ressources, Conflits, puis Simulation depuis un conflit disponible
- Navigation Collaborateur vers Mon Planning et ouverture de la popup de suivi si une tache est visible
- Ouverture des notifications et clic sur une notification si elle existe

Les tests dependants de donnees optionnelles utilisent des assumptions JUnit : si aucun conflit, aucune tache ou aucune notification n'est disponible, la sous-partie concernee est ignoree proprement.
