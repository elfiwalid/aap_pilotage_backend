# STAFF2STAFF ML - Resource Forecasting Dataset

Ce dossier contient uniquement les artefacts de preparation de donnees pour le futur module ML de prediction des besoins en ressources.

## Objectif

Construire un dataset mensuel par projet a partir des tables existantes :

- `projets`
- `previsions`
- `affectations`
- `anomalies_v2`
- `users` pour les collaborateurs

Le dataset cible est genere dans :

```text
ml/datasets/resource_forecasting_dataset.csv
```

## Colonnes generees

- `projet_id`
- `mois`
- `annee`
- `duree_projet_jours`
- `nb_collaborateurs_actuels`
- `charge_moyenne`
- `charge_max`
- `nb_conflits`
- `nb_surcharges`
- `nb_sous_charges`
- `nb_anomalies_total`
- `nb_collaborateurs_concernes`
- `target_besoin_ressources_mois_suivant`

## Generation

Depuis le dossier `aap_pilotage_backend`, executer :

```bash
psql -h localhost -U postgres -d pfe -f ml/scripts/extract_resource_forecasting_dataset.sql
```

Le script utilise la base configuree dans `src/main/resources/application.properties`.

## Donnees synthetiques

Le dataset reel ne contient actuellement que 2 lignes projet-mois exploitables. Ce volume est insuffisant pour entrainer un modele ML fiable : il ne couvre pas assez de projets, de mois, de variations de charge, ni de situations d'anomalies.

Pour la phase PFE d'entrainement/demonstration ML, un generateur produit un dataset synthetique separe, sans inserer de donnees dans PostgreSQL et sans modifier la logique Spring Boot.

Depuis le dossier `aap_pilotage_backend`, executer :

```bash
python ml/scripts/generate_synthetic_resource_dataset.py
```

Le CSV synthetique est genere ici :

```text
ml/datasets/synthetic_resource_forecasting_dataset.csv
```

Ce fichier conserve les memes colonnes que le dataset reel. Il genere environ 5000 lignes projet-mois avec une simulation temporelle par projet :

- chaque projet possede une tendance cachee : croissance, stabilite ou decroissance ;
- chaque projet evolue mois par mois avec un effectif courant et une demande latente ;
- la cible correspond a l'effectif simule du mois suivant, puis ajuste par des revisions cachees ;
- des facteurs non presents dans les features influencent la cible : budget, priorite client, disponibilite RH et phase projet ;
- des cas atypiques sont introduits : ramp-up client urgent, gel budget, reduction de scope, arbitrage staffing ;
- un bruit controle rend la relation moins deterministe ;
- la cible reste toujours superieure ou egale a 1 ;
- `nb_anomalies_total` est toujours egal a `nb_conflits + nb_surcharges + nb_sous_charges` ;
- `nb_collaborateurs_concernes` ne depasse jamais `nb_collaborateurs_actuels` ;
- `charge_max` reste toujours superieure ou egale a `charge_moyenne`.

Limites : ces donnees servent uniquement a amorcer l'entrainement et la demonstration du module ML PFE. Elles ne remplacent pas un historique de production reel et peuvent introduire des biais lies aux hypotheses de generation. Les facteurs caches rendent le probleme plus realiste, mais ils restent simules.

## Entrainement du modele

Le script d'entrainement compare deux modeles scikit-learn :

- `Linear Regression`
- `Random Forest Regressor`

Depuis le dossier `aap_pilotage_backend`, executer :

```bash
python ml/scripts/train_resource_forecasting_model.py
```

Le script charge :

```text
ml/datasets/synthetic_resource_forecasting_dataset.csv
```

Il sauvegarde le meilleur modele ici :

```text
ml/models/resource_forecasting_model.pkl
```

Les metriques sont sauvegardees ici :

```text
ml/models/resource_forecasting_metrics.json
```

Resultats initiaux obtenus avec l'ancien generateur, trop deterministe :

| Modele | MAE | RMSE | R2 |
| --- | ---: | ---: | ---: |
| Linear Regression | 1.4422 | 2.9547 | 0.9957 |
| Random Forest Regressor | 1.5184 | 2.9444 | 0.9957 |

Ces scores etaient artificiellement eleves car la target etait calculee presque directement a partir des features du meme mois.

Resultats apres generation temporelle plus realiste, sur 5000 lignes synthetiques avec 20% du dataset en test :

| Modele | MAE | RMSE | R2 |
| --- | ---: | ---: | ---: |
| Linear Regression | 11.5775 | 22.4014 | 0.8936 |
| Random Forest Regressor | 11.8229 | 23.2256 | 0.8857 |

Le meilleur modele retenu est `Linear Regression`, selectionne sur le RMSE le plus faible.

Limite importante : meme avec un R2 plus credible, ces metriques mesurent encore la capacite du modele a apprendre un monde synthetique. Elles ne prouvent pas encore une performance fiable sur des donnees reelles STAFF2STAFF. Une validation serieuse devra etre refaite lorsque l'application aura accumule un historique reel suffisant.

## Hypotheses de preparation

- Une ligne correspond a un couple `projet_id` / mois / annee.
- Les mois candidats sont derives des periodes de `previsions` et des plages de dates des `affectations`.
- La duree projet est calculee par `projets.date_fin - projets.date_debut`.
- Les collaborateurs actuels sont les collaborateurs distincts affectes au projet pendant le mois.
- La charge moyenne et la charge maximale utilisent `taux_affectation` en priorite, puis `charge_prevue` si le taux est absent.
- Les anomalies V2 sont rattachees a un projet par correspondance entre `anomalies_v2.projets_concernes` et `projets.nom`, car `AnomalieV2` ne porte pas actuellement de cle etrangere directe vers `Projet`.
- La cible `target_besoin_ressources_mois_suivant` est le nombre de collaborateurs distincts affectes au meme projet sur le mois suivant.

## Etat actuel des donnees reelles

Audit realise sur la base locale `pfe` :

- projets : 2
- previsions : 2
- affectations : 897
- anomalies V2 : 590
- collaborateurs : 328
- lignes projet-mois candidates : 2

Ce volume est insuffisant pour entrainer un modele ML fiable. Les 897 affectations et 590 anomalies donnent de la matiere metier, mais seulement 2 observations temporelles projet-mois ne permettent pas d'apprendre une relation generalisable.

La phase suivante recommandee est de generer un dataset synthetique realiste, calibre sur les distributions observees des donnees reelles, avec au moins plusieurs milliers de lignes projet-mois.
