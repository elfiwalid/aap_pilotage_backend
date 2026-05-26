Voici un **cahier des charges détaillé** pour ton projet complet. Tu peux l’utiliser comme base pour ton rapport, ton backend Spring Boot, ou pour expliquer le projet à ton encadrant.

---

# Cahier des charges détaillé

## Projet : Application de gestion du staffing, des prévisions V2 et des anomalies

---

# 1. Présentation générale du projet

Le projet consiste à développer une application interne destinée à une entreprise afin de faciliter la gestion des projets, des collaborateurs, des prévisions V2, du staffing, des anomalies et des conflits d’affectation.

Le frontend de l’application est déjà organisé et structuré. L’objectif actuel est de développer et connecter la partie backend avec **Java Spring Boot**.

L’application doit permettre à chaque utilisateur d’accéder à un espace adapté à son rôle. Trois profils principaux sont définis :

```text
1. Chef de Projet
2. Resource Manager
3. Collaborateur
```

Chaque profil dispose de fonctionnalités spécifiques selon ses responsabilités dans l’entreprise.

Le développement backend sera réalisé progressivement. La première étape concerne l’authentification, puis le développement commencera par le profil **Chef de Projet**.

---

# 2. Objectifs du projet

## Objectif principal

L’objectif principal de l’application est de centraliser la gestion des ressources et des projets afin de mieux suivre le taux de staffing des collaborateurs, détecter les anomalies d’affectation et faciliter la prise de décision.

---

## Objectifs spécifiques

L’application doit permettre de :

```text
- Authentifier les utilisateurs selon leur rôle.
- Gérer les projets d’un Chef de Projet.
- Importer ou créer manuellement des projets.
- Importer des fichiers de prévision V2.
- Visualiser les KPI liés aux projets et aux ressources.
- Détecter les anomalies d’affectation.
- Gérer les conflits entre projets.
- Proposer des ressources alternatives.
- Réaliser des simulations de type “what-if”.
- Visualiser l’historique des rapports V2.
- Gérer les notifications entre utilisateurs.
- Permettre aux collaborateurs de visualiser leurs projets et leur planning.
- Définir les jours ouvrables utilisés dans les calculs.
```

---

# 3. Périmètre du projet

## Fonctionnalités incluses

Le projet couvre les modules suivants :

```text
- Authentification
- Gestion des utilisateurs et des rôles
- Dashboard Chef de Projet
- Gestion des projets
- Import des fichiers de prévision V2
- Analyse des anomalies
- Historique des rapports V2
- Dashboard Resource Manager
- Gestion des ressources
- Visualisation des ressources en tableau et heatmap
- Gestion des conflits
- Simulations what-if
- Dashboard Collaborateur
- Planning collaborateur
- Notifications
- Profil utilisateur
- Paramètres des jours ouvrables
```

---

## Fonctionnalités non prioritaires dans la première version

Certaines fonctionnalités peuvent être prévues plus tard :

```text
- Messagerie instantanée entre Chefs de Projet
- Historique très détaillé de toutes les modifications
- Workflow de validation complexe
- Export PDF avancé
- Intelligence artificielle avancée pour la recommandation automatique
- Intégration complète avec Active Directory en production
```

Pour la première version, l’objectif est de mettre en place une base fonctionnelle, claire et évolutive.

---

# 4. Acteurs du système

## 4.1 Chef de Projet

Le Chef de Projet est responsable de la gestion de ses projets. Il peut consulter ses KPI, visualiser ses projets, ajouter un projet, importer une prévision V2, analyser les anomalies et consulter les rapports V2.

---

## 4.2 Resource Manager

Le Resource Manager est responsable de la gestion globale des ressources. Il visualise les collaborateurs, les projets, les conflits, les anomalies et peut proposer des ressources alternatives.

---

## 4.3 Collaborateur

Le Collaborateur peut consulter ses projets, son planning, ses tâches, ses KPI personnels et ses notifications.

---

# 5. Authentification et gestion des rôles

## 5.1 Description

L’application doit proposer un système d’authentification permettant à chaque utilisateur de se connecter et d’accéder uniquement aux fonctionnalités correspondant à son rôle.

Les rôles sont :

```text
CHEF_PROJET
RESOURCE_MANAGER
COLLABORATEUR
```

---

## 5.2 Fonctionnement attendu

Après connexion :

```text
- Si l’utilisateur est Chef de Projet, il est redirigé vers l’espace Chef de Projet.
- Si l’utilisateur est Resource Manager, il est redirigé vers l’espace Resource Manager.
- Si l’utilisateur est Collaborateur, il est redirigé vers l’espace Collaborateur.
```

---

## 5.3 Gestion technique

Dans la première version, une entité unique `User` sera utilisée avec un champ `role`.

Exemple :

```text
User
- id
- nom
- prenom
- email
- departement
- poste
- matricule
- competences
- tauxStaffing
- disponible
- actif
- role
```

Cette approche est simple et adaptée, car les profils partagent les mêmes informations principales. Les différences se feront surtout au niveau des permissions, des interfaces et des endpoints backend.

---

# 6. Profil Chef de Projet

Le Chef de Projet dispose de plusieurs interfaces.

---

## 6.1 Dashboard Chef de Projet

### Description

Le dashboard permet au Chef de Projet de visualiser une synthèse de ses projets, de ses ressources et des anomalies détectées.

### Fonctionnalités

Le dashboard doit afficher des KPI comme :

```text
- Nombre total de projets gérés
- Nombre de projets en cours
- Nombre de projets terminés
- Nombre total de collaborateurs affectés
- Nombre d’anomalies détectées
- Nombre d’anomalies ouvertes
- Nombre d’anomalies résolues
- Taux moyen de staffing
- Nombre de rapports V2 disponibles
```

### Objectif

Donner au Chef de Projet une vision rapide de l’état de ses projets et des problèmes à traiter.

---

## 6.2 Interface “Mes projets”

### Description

Cette interface permet au Chef de Projet de visualiser l’ensemble des projets qu’il gère.

### Actions disponibles

L’interface contient trois boutons principaux :

```text
1. Récupérer les projets existants
2. Analyser les anomalies
3. Ajouter un projet
```

---

### 6.2.1 Récupérer les projets existants

Cette action permet de charger tous les projets associés au Chef de Projet connecté.

Les projets affichés peuvent contenir :

```text
- Nom du projet
- Description
- Date de début
- Date de fin
- Statut
- Nombre de collaborateurs affectés
- Nombre d’anomalies
- Taux de staffing global du projet
```

---

### 6.2.2 Analyser les anomalies

Cette action lance une analyse des affectations liées aux projets du Chef de Projet.

L’analyse peut détecter :

```text
- Collaborateur surchargé
- Collaborateur affecté à plusieurs projets en même temps
- Taux de staffing supérieur à 100 %
- Ressource manquante
- Incohérence entre prévision V2 et affectations réelles
- Collaborateur non disponible
```

---

### 6.2.3 Ajouter un projet

Lorsqu’il clique sur “Ajouter un projet”, le Chef de Projet peut choisir entre deux méthodes :

```text
1. Remplir un formulaire manuellement
2. Importer un fichier de prévision V2
```

---

## 6.3 Ajout manuel d’un projet

### Description

Le Chef de Projet peut créer un projet via un formulaire.

### Champs possibles

```text
- Nom du projet
- Description
- Date de début
- Date de fin
- Statut du projet
- Collaborateurs affectés
- Taux d’affectation des collaborateurs
- Charge prévue
```

### Règles métier

```text
- Le nom du projet est obligatoire.
- La date de début doit être inférieure à la date de fin.
- Le projet doit être lié au Chef de Projet connecté.
- Le taux d’affectation d’un collaborateur ne doit pas dépasser 100 % sur une même période.
```

---

## 6.4 Import d’un fichier de prévision V2

### Description

Le Chef de Projet peut importer un fichier de prévision V2 pour créer ou mettre à jour les projets et les affectations.

### Fonctionnalités

```text
- Télécharger un template V2
- Importer un fichier V2
- Vérifier le format du fichier
- Lire les données du fichier
- Créer ou mettre à jour les projets
- Créer les affectations des collaborateurs
- Enregistrer la prévision importée
- Déclencher l’analyse des anomalies
```

---

### Types de prévisions V2

L’application doit gérer deux types de prévisions :

```text
- Prévision trimestrielle
- Prévision annuelle
```

---

### Règles métier

```text
- Une prévision V2 est importée par un Chef de Projet.
- Une prévision V2 est liée à un ou plusieurs projets.
- Une prévision V2 peut être trimestrielle ou annuelle.
- Une nouvelle prévision peut remplacer ou désactiver une ancienne prévision active.
- Le fichier doit respecter le template fourni.
```

---

## 6.5 Interface “Anomalies”

### Description

Cette interface permet au Chef de Projet de consulter les anomalies liées aux collaborateurs affectés à ses projets.

### Informations affichées

```text
- Titre de l’anomalie
- Type d’anomalie
- Description
- Projet concerné
- Collaborateur concerné
- Date de détection
- Statut de l’anomalie
```

### Types d’anomalies

```text
- Surcharge
- Conflit d’affectation
- Disponibilité insuffisante
- Taux incohérent
- Ressource manquante
```

### Statuts possibles

```text
- Ouverte
- En cours
- Résolue
- Rejetée
```

---

## 6.6 Interface “Rapports”

### Description

Cette interface permet au Chef de Projet de visualiser l’historique des rapports V2.

### Types de rapports

```text
- Rapport trimestriel : début en janvier
- Rapport annuel : début en avril
```

### Informations affichées

```text
- Titre du rapport
- Type du rapport
- Période de début
- Période de fin
- Date de génération
- Projet concerné
- Prévision associée
```

---

## 6.7 Notifications Chef de Projet

### Description

Le Chef de Projet reçoit des notifications liées aux anomalies, aux conflits ou aux messages envoyés par le Resource Manager.

### Exemples de notifications

```text
- Nouvelle anomalie détectée
- Conflit d’affectation avec un autre projet
- Proposition d’une ressource alternative
- Demande de négociation envoyée par le Resource Manager
```

---

## 6.8 Profil Chef de Projet

Le Chef de Projet peut consulter ses informations personnelles :

```text
- Nom
- Prénom
- Email
- Département
- Poste
- Rôle
- Projets gérés
```

---

# 7. Profil Resource Manager

Le Resource Manager dispose d’une vision globale sur les ressources, les projets, le staffing et les anomalies.

---

## 7.1 Dashboard Resource Manager

### Description

Le dashboard permet au Resource Manager de visualiser les KPI globaux de l’entreprise ou de son périmètre.

### KPI possibles

```text
- Nombre total de collaborateurs
- Nombre de collaborateurs disponibles
- Nombre de collaborateurs affectés
- Nombre de collaborateurs surchargés
- Taux moyen de staffing
- Nombre total de projets
- Nombre d’anomalies ouvertes
- Nombre de conflits actifs
- Nombre de ressources alternatives proposées
```

---

## 7.2 Interface “Ressources”

### Description

Cette interface permet de visualiser les collaborateurs.

Deux modes d’affichage doivent être disponibles :

```text
1. Tableau
2. Heatmap
```

---

### 7.2.1 Affichage tableau

Le tableau doit afficher :

```text
- Nom
- Prénom
- Email
- Matricule
- Poste
- Compétences
- Taux de staffing
- Disponibilité
- Nombre de projets affectés
```

---

### 7.2.2 Affichage heatmap

La heatmap permet de visualiser le taux de staffing des collaborateurs selon les périodes V2.

Elle doit représenter :

```text
- Les collaborateurs
- Les périodes trimestrielles
- Les périodes annuelles
- Le taux de staffing par période
```

Exemple de logique :

```text
Vert   : collaborateur disponible
Orange : collaborateur fortement occupé
Rouge  : collaborateur surchargé
```

---

## 7.3 Interface “Projets”

### Description

Cette interface permet au Resource Manager de visualiser l’ensemble des projets de l’entreprise.

### Informations affichées

```text
- Nom du projet
- Chef de Projet responsable
- Date de début
- Date de fin
- Statut
- Nombre de collaborateurs
- Nombre d’anomalies
- Taux de staffing global
```

---

## 7.4 Interface “Conflits”

### Description

Cette interface permet au Resource Manager de gérer les conflits liés aux affectations des collaborateurs.

Un conflit peut apparaître lorsqu’un collaborateur est affecté à plusieurs projets avec une charge totale trop élevée.

---

### Fonctionnalités

Le Resource Manager peut :

```text
- Visualiser les conflits détectés
- Voir les détails des anomalies
- Identifier les projets concernés
- Identifier les collaborateurs concernés
- Notifier les Chefs de Projet concernés
- Proposer des ressources alternatives
- Suivre le statut du conflit
```

---

### Objectif

Le Resource Manager ne résout pas toujours directement le conflit. Il facilite la négociation entre les Chefs de Projet concernés.

---

## 7.5 Interface “Simulations”

### Description

Cette interface permet de réaliser des simulations de type “what-if”.

L’objectif est de proposer des ressources disponibles et qualifiées pour résoudre les anomalies.

---

### Fonctionnalités

Le Resource Manager peut :

```text
- Sélectionner un projet en anomalie
- Sélectionner une ressource surchargée
- Rechercher des ressources alternatives
- Filtrer par compétence
- Filtrer par disponibilité
- Simuler un changement d’affectation
- Visualiser l’impact sur le staffing
```

---

### Exemple

Si un collaborateur Java est surchargé à 120 %, le système peut proposer un autre collaborateur ayant la compétence Java et un taux de staffing inférieur à 80 %.

---

## 7.6 Notifications Resource Manager

Le Resource Manager peut :

```text
- Recevoir des notifications d’anomalies
- Envoyer des notifications aux Chefs de Projet
- Suivre les réponses ou actions
- Notifier une proposition de ressource alternative
```

---

## 7.7 Profil Resource Manager

Le Resource Manager peut consulter :

```text
- Nom
- Prénom
- Email
- Département
- Poste
- Rôle
```

---

## 7.8 Paramètres

### Description

L’interface paramètres permet de définir les jours ouvrables utilisés dans les calculs.

### Jours paramétrables

```text
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
```

### Utilisation

Ces paramètres seront utilisés pour :

```text
- Calculer la disponibilité
- Calculer le taux de staffing
- Gérer le planning
- Réaliser les simulations
```

---

# 8. Profil Collaborateur

Le Collaborateur dispose d’un espace personnel lui permettant de suivre ses projets, son planning et ses KPI.

---

## 8.1 Dashboard Collaborateur

### Description

Le dashboard permet au Collaborateur de visualiser ses informations principales.

### KPI possibles

```text
- Nombre de projets actifs
- Taux de staffing personnel
- Nombre de tâches en cours
- Nombre de tâches terminées
- Charge de travail prévue
- Disponibilité
```

---

## 8.2 Interface “Mes projets”

### Description

Cette interface permet au Collaborateur de visualiser les projets auxquels il est affecté.

### Informations affichées

```text
- Nom du projet
- Chef de Projet responsable
- Date de début
- Date de fin
- Rôle dans le projet
- Taux d’affectation
- Statut du projet
```

---

## 8.3 Interface “Mon planning”

### Description

Cette interface contient un calendrier permettant au Collaborateur de visualiser et modifier les tâches liées aux projets auxquels il participe.

### Fonctionnalités

Le Collaborateur peut :

```text
- Visualiser ses tâches dans un calendrier
- Ajouter ou modifier certaines tâches
- Voir les périodes d’affectation
- Suivre les tâches liées à chaque projet
```

---

### Informations d’une tâche

```text
- Titre
- Description
- Date de début
- Date de fin
- Projet concerné
- Statut
```

---

## 8.4 Notifications Collaborateur

Le Collaborateur peut recevoir des notifications comme :

```text
- Nouvelle affectation à un projet
- Modification d’un planning
- Nouvelle tâche
- Changement de statut d’un projet
```

---



# 10. Règles métier principales

## 10.1 Règles liées aux rôles

```text
- Un Chef de Projet peut gérer uniquement ses propres projets.
- Un Resource Manager peut consulter tous les projets et toutes les ressources.
- Un Collaborateur peut consulter uniquement ses propres projets et son planning.
```

---

## 10.2 Règles liées au projet

```text
- Un projet doit toujours être lié à un Chef de Projet.
- Un projet doit avoir une date de début et une date de fin.
- La date de fin doit être supérieure à la date de début.
- Un projet peut être créé manuellement ou à partir d’une prévision V2.
```

---

## 10.3 Règles liées au staffing

```text
- Le taux de staffing d’un collaborateur est calculé à partir de ses affectations.
- Si un collaborateur n’a aucune affectation, son taux de staffing est 0 %.
- Si un collaborateur dépasse 100 %, une anomalie de surcharge est créée.
- Si un collaborateur est affecté à plusieurs projets sur la même période, une vérification de conflit est réalisée.
```

---

## 10.4 Règles liées aux anomalies

```text
- Une anomalie doit être liée à un projet.
- Une anomalie peut être liée à un collaborateur.
- Une anomalie nouvellement détectée a le statut OUVERTE.
- Une anomalie peut passer à EN_COURS, RESOLUE ou REJETEE.
```

---

## 10.5 Règles liées aux notifications

```text
- Une notification possède un expéditeur et un destinataire.
- Une notification peut être liée à une anomalie.
- Une notification est NON_LUE par défaut.
- L’utilisateur peut marquer une notification comme LUE.
```

---

## 10.6 Règles liées aux prévisions V2

```text
- Une prévision V2 peut être trimestrielle ou annuelle.
- Une prévision est importée par un Chef de Projet.
- Une prévision contient une période de début et une période de fin.
- Une prévision peut être active ou inactive.
- Lorsqu’une nouvelle prévision est importée pour le même projet et la même période, l’ancienne peut devenir inactive.
```



# 13. Connexion avec le frontend

Le frontend communiquera avec le backend via des appels HTTP REST.

Exemple :

```text
Frontend React / Angular / Vue
        ↓
HTTP Request
        ↓
Spring Boot Controller
        ↓
Service
        ↓
Repository
        ↓
PostgreSQL
```

---



# 14. Découpage du développement

## Étape 1 : Initialisation backend

```text
- Créer le projet Spring Boot
- Configurer PostgreSQL
- Configurer application.properties
- Créer les entités
- Créer les repositories
- Vérifier la création des tables
```

---

## Étape 2 : Authentification

```text
- Créer la logique de connexion
- Gérer les rôles
- Créer /api/auth/login
- Créer /api/auth/me
- Préparer la sécurité des endpoints
```

---

## Étape 3 : Profil Chef de Projet

```text
- Dashboard Chef de Projet
- Liste des projets
- Ajout manuel d’un projet
- Import V2
- Téléchargement template V2
- Analyse des anomalies
- Historique des rapports
- Notifications
- Profil
```

---

## Étape 4 : Profil Resource Manager

```text
- Dashboard global
- Liste des ressources
- Heatmap staffing
- Liste des projets
- Gestion des conflits
- Détails des anomalies
- Notifications aux Chefs de Projet
- Simulations what-if
- Paramètres des jours ouvrables
```

---

## Étape 5 : Profil Collaborateur

```text
- Dashboard personnel
- Mes projets
- Mon planning
- Gestion des tâches
- Notifications
- Profil
```

---

## Étape 6 : Tests et intégration frontend

```text
- Tester les endpoints avec Postman
- Connecter les services frontend
- Vérifier les erreurs CORS
- Tester les rôles
- Tester les formulaires
- Tester l’import V2
- Tester les dashboards
```

---

# 15. Priorité de développement recommandée

Pour avancer proprement, je te conseille cet ordre :

```text
1. User + Role
2. Projet
3. Affectation
4. Prévision V2
5. Anomalie
6. Rapport V2
7. Notification
8. Dashboard Chef de Projet
9. Resource Manager
10. Collaborateur
```

---

# 16. Conclusion

Ce projet vise à construire une application interne complète pour améliorer la gestion des projets et des ressources dans l’entreprise.

L’application permettra aux Chefs de Projet de suivre leurs projets et leurs anomalies, aux Resource Managers de gérer les conflits et les ressources, et aux Collaborateurs de suivre leurs affectations et leur planning.

Le backend Spring Boot sera conçu avec une architecture propre, basée sur des entités métier, des DTO, des services, des repositories et des controllers REST.

La première étape du développement sera l’authentification, suivie par le développement du profil Chef de Projet.





Oui, il faut ajouter ce **workflow métier principal**, parce que c’est le cœur de ton application.

Voici une version claire et détaillée à intégrer dans ton cahier des charges.

---

# Workflow métier principal

## 1. Objectif du workflow

Le workflow principal de l’application permet de gérer le cycle complet suivant :

```text
Création du projet
→ Import de la prévision V2
→ Analyse automatique des affectations
→ Détection des anomalies et conflits
→ Intervention du Resource Manager
→ Notification des Chefs de Projet
→ Correction ou négociation
→ Simulation What-if avec IA
→ Proposition de ressources alternatives
```

Ce workflow permet à l’entreprise d’avoir une vision claire sur l’utilisation des collaborateurs, d’éviter la surcharge, d’identifier le sous-staffing et de faciliter la prise de décision.

---

# 2. Workflow côté Chef de Projet

## 2.1 Création d’un projet

Le Chef de Projet commence par créer un projet.

Il peut le créer de deux manières :

```text
1. Création manuelle via un formulaire
2. Création/import via un fichier de prévision V2
```

Dans le cas d’une création manuelle, le Chef de Projet renseigne les informations principales :

```text
- Nom du projet
- Description
- Date de début
- Date de fin
- Statut
- Collaborateurs prévus
- Périodes d’affectation
- Taux ou charge d’affectation
```

---

## 2.2 Import de la prévision V2

Après la création du projet, le Chef de Projet importe sa prévision V2.

La prévision V2 contient les informations prévisionnelles liées aux ressources et aux affectations.

Elle peut contenir par exemple :

```text
- Projet concerné
- Collaborateurs affectés
- Date de début d’affectation
- Date de fin d’affectation
- Charge prévue
- Taux d’affectation
- Type de prévision : trimestrielle ou annuelle
```

L’application doit vérifier le fichier importé :

```text
- Format correct
- Colonnes obligatoires présentes
- Dates valides
- Collaborateurs existants
- Taux d’affectation cohérent
- Projet associé valide
```

Si le fichier est valide, le système enregistre la prévision et met à jour les affectations du projet.

---

# 3. Analyse automatique après import V2

Après chaque import de prévision V2 par un Chef de Projet, le système lance automatiquement une analyse des affectations.

Cette analyse a pour objectif de détecter :

```text
- Les surcharges
- Le sous-staffing
- Les conflits entre projets
- Les incohérences de dates
- Les collaborateurs affectés à plusieurs projets en même temps
- Les ressources manquantes
- Les taux d’affectation incohérents
```

---

# 4. Détection automatique côté Resource Manager

Lorsque plusieurs Chefs de Projet importent leurs prévisions V2, le Resource Manager obtient automatiquement une vision consolidée.

Le système compare les prévisions de tous les projets afin de détecter les conflits entre collaborateurs.

---

## 4.1 Exemple de conflit de surcharge

Un collaborateur est affecté sur plusieurs projets pendant la même période.

Exemple :

```text
Collaborateur : Ahmed
Projet A : 70 %
Projet B : 60 %

Total : 130 %
```

Dans ce cas, le système détecte une anomalie :

```text
Type : SURCHARGE
Cause : taux total supérieur à 100 %
```

---

## 4.2 Exemple de sous-staffing

Un projet a besoin d’un certain nombre de ressources ou d’une charge minimale, mais la prévision importée ne couvre pas le besoin.

Exemple :

```text
Projet A besoin : 5 collaborateurs Java
Projet A affectés : 3 collaborateurs Java
```

Le système détecte :

```text
Type : SOUS_STAFFING
Cause : ressources insuffisantes par rapport au besoin prévu
```

---

## 4.3 Exemple de conflit de période

Un collaborateur est affecté à deux projets sur la même période avec une disponibilité insuffisante.

Exemple :

```text
Projet A : du 01/06/2026 au 30/06/2026
Projet B : du 15/06/2026 au 15/07/2026
```

Le système détecte que les périodes se chevauchent et calcule la charge totale pendant cette période.

---

# 5. Types d’anomalies à gérer

Il faut ajouter ces types dans ton cahier des charges et dans ton enum `TypeAnomalie`.

```text
SURCHARGE
SOUS_STAFFING
CONFLIT_PERIODE
COLLABORATEUR_INDISPONIBLE
TAUX_INCOHERENT
RESSOURCE_MANQUANTE
COMPETENCE_INSUFFISANTE
```

---

## Description des anomalies

| Type                         | Description                                                              |
| ---------------------------- | ------------------------------------------------------------------------ |
| `SURCHARGE`                  | Le collaborateur dépasse 100 % de taux d’affectation sur une période.    |
| `SOUS_STAFFING`              | Le projet n’a pas assez de ressources par rapport au besoin prévu.       |
| `CONFLIT_PERIODE`            | Deux affectations se chevauchent sur la même période.                    |
| `COLLABORATEUR_INDISPONIBLE` | Le collaborateur est affecté alors qu’il n’est pas disponible.           |
| `TAUX_INCOHERENT`            | Le taux d’affectation est négatif, nul ou supérieur à une limite métier. |
| `RESSOURCE_MANQUANTE`        | Une ressource prévue dans la prévision n’existe pas dans la base.        |
| `COMPETENCE_INSUFFISANTE`    | Le collaborateur affecté ne possède pas la compétence requise.           |

---

# 6. Rôle du Resource Manager dans le workflow

Le Resource Manager reçoit automatiquement les anomalies détectées.

Il peut ensuite :

```text
- Visualiser les conflits détectés
- Filtrer les anomalies par type
- Voir les projets concernés
- Voir les Chefs de Projet concernés
- Voir les collaborateurs en conflit
- Analyser les périodes de chevauchement
- Notifier les Chefs de Projet
- Proposer une correction
- Proposer une ressource alternative
- Lancer une simulation What-if
```

Le Resource Manager joue donc un rôle de coordination entre les Chefs de Projet.

---

# 7. Notification des Chefs de Projet

Lorsqu’un conflit est détecté, le Resource Manager peut notifier les Chefs de Projet concernés.

La notification peut contenir :

```text
- Type d’anomalie
- Collaborateur concerné
- Projets concernés
- Période du conflit
- Taux total détecté
- Message du Resource Manager
- Proposition de correction
```

Exemple de notification :

```text
Titre : Conflit de surcharge détecté

Message :
Le collaborateur Ahmed est affecté à 130 % entre le 01/06/2026 et le 30/06/2026 sur les projets Projet A et Projet B. Merci de revoir les affectations ou de négocier avec l’autre Chef de Projet concerné.
```

---

# 8. Correction par les Chefs de Projet

Après réception de la notification, les Chefs de Projet peuvent corriger leurs prévisions.

Ils peuvent :

```text
- Modifier la période d’affectation
- Réduire le taux d’affectation
- Remplacer le collaborateur
- Supprimer une affectation
- Réimporter une nouvelle prévision V2 corrigée
```

Après correction, le système relance l’analyse automatique.

Si l’anomalie n’existe plus, elle passe au statut :

```text
RESOLUE
```

Sinon, elle reste :

```text
OUVERTE
```

ou :

```text
EN_COURS
```

---

# 9. Fonctionnalité What-if avec IA

## 9.1 Objectif

La fonctionnalité **What-if** permet au Resource Manager de simuler différentes solutions pour résoudre les anomalies détectées.

L’objectif est d’aider le Resource Manager à prendre une décision plus rapide et plus fiable.

---

## 9.2 Principe

Le Resource Manager sélectionne une anomalie ou un conflit.

Le système analyse :

```text
- Le collaborateur concerné
- Ses compétences
- Ses affectations existantes
- Sa disponibilité
- La période du conflit
- Les besoins du projet
- Les autres collaborateurs disponibles
```

Puis l’IA ou l’algorithme de simulation propose des alternatives.

---

## 9.3 Exemples de simulations

### Simulation 1 : remplacement d’un collaborateur surchargé

```text
Problème :
Ahmed est affecté à 130 %.

Simulation :
Trouver un collaborateur avec les mêmes compétences, disponible pendant la même période, avec un taux de staffing inférieur à 80 %.
```

Résultat attendu :

```text
Proposition :
Remplacer Ahmed par Sara sur le Projet B.
Sara possède les compétences Java/Spring Boot et son taux de staffing est de 40 %.
```

---

### Simulation 2 : réduction du taux d’affectation

```text
Problème :
Ahmed est affecté à 130 %.

Simulation :
Réduire son taux sur le Projet B de 60 % à 30 %.
```

Résultat attendu :

```text
Nouveau taux total : 100 %
Conflit résolu
```

---

### Simulation 3 : décalage de période

```text
Problème :
Deux projets utilisent le même collaborateur sur la même période.

Simulation :
Décaler l’affectation du Projet B de deux semaines.
```

Résultat attendu :

```text
Plus de chevauchement critique
Conflit réduit ou résolu
```

---

### Simulation 4 : ajout d’une ressource alternative

```text
Problème :
Projet sous-staffé.

Simulation :
Ajouter un collaborateur disponible avec les compétences nécessaires.
```

Résultat attendu :

```text
Le projet passe de 3 ressources disponibles à 5 ressources disponibles.
Sous-staffing résolu.
```

---

# 10. Données utilisées par le module IA / What-if

Le module de simulation peut utiliser :

```text
- Liste des collaborateurs
- Compétences
- Taux de staffing
- Disponibilités
- Affectations existantes
- Dates des projets
- Historique des anomalies
- Besoins du projet
- Jours ouvrables
```

---

# 11. Suggestions à ajouter au workflow

Je te conseille d’ajouter aussi ces éléments, car ils rendent ton projet plus professionnel.

---

## 11.1 Statut du cycle de vie d’une anomalie

Une anomalie peut avoir plusieurs statuts :

```text
OUVERTE
EN_ANALYSE
NOTIFIEE
EN_CORRECTION
RESOLUE
REJETEE
```

Explication :

```text
OUVERTE       : anomalie détectée automatiquement
EN_ANALYSE    : Resource Manager consulte l’anomalie
NOTIFIEE      : notification envoyée aux Chefs de Projet
EN_CORRECTION : correction en cours par les Chefs de Projet
RESOLUE       : anomalie corrigée
REJETEE       : anomalie considérée non pertinente
```

---

## 11.2 Historique des actions

Il est utile d’avoir un historique simple des actions :

```text
- Date de détection de l’anomalie
- Date de notification
- Date de correction
- Utilisateur ayant effectué l’action
- Commentaire éventuel
```

Même si tu ne crées pas tout de suite une entité séparée, tu peux le mentionner dans le cahier des charges comme amélioration ou évolution.

---

## 11.3 Priorité des anomalies

Toutes les anomalies n’ont pas la même importance.

Tu peux ajouter une priorité :

```text
FAIBLE
MOYENNE
ELEVEE
CRITIQUE
```

Exemple :

```text
Taux de staffing à 105 % → priorité moyenne
Taux de staffing à 150 % → priorité critique
Projet stratégique sous-staffé → priorité élevée
```

---

## 11.4 Recommandation de ressources alternatives

Le système peut proposer des ressources alternatives selon plusieurs critères :

```text
- Compétences similaires
- Disponibilité sur la période
- Taux de staffing actuel
- Département
- Expérience
- Historique de projets
```

Pour une première version, tu peux commencer avec une logique simple :

```text
Compétence correspondante + taux de staffing inférieur à 80 %
```

Puis plus tard, ajouter l’IA.

---

# 12. Workflow complet à intégrer dans le cahier des charges

Voici une version prête à coller dans ton document.

---

## Workflow de gestion des prévisions, anomalies et conflits

Le workflow principal de l’application commence par la création d’un projet par le Chef de Projet. Une fois le projet créé, le Chef de Projet importe une prévision V2 contenant les ressources prévues, les périodes d’affectation et les taux de staffing associés.

Après chaque import de prévision V2, le système lance automatiquement une analyse des affectations. Cette analyse permet de détecter les anomalies liées aux collaborateurs, comme la surcharge, le sous-staffing, les conflits de période, les collaborateurs indisponibles, les taux incohérents ou les ressources manquantes.

Lorsque plusieurs Chefs de Projet importent leurs prévisions, le système consolide l’ensemble des données afin d’identifier les conflits entre projets. Par exemple, si un même collaborateur est affecté sur plusieurs projets pendant la même période avec un taux total supérieur à 100 %, une anomalie de surcharge est automatiquement créée.

Les anomalies détectées sont visibles par le Resource Manager. Celui-ci peut consulter les détails de chaque anomalie, identifier les projets et les collaborateurs concernés, puis notifier les Chefs de Projet impliqués afin qu’ils corrigent leurs prévisions ou négocient entre eux.

Le Resource Manager peut également utiliser une fonctionnalité de simulation What-if avec IA. Cette fonctionnalité analyse les collaborateurs disponibles, leurs compétences, leurs taux de staffing et les besoins des projets afin de proposer des solutions possibles. Les solutions peuvent être le remplacement d’un collaborateur surchargé, la réduction d’un taux d’affectation, le décalage d’une période ou l’ajout d’une ressource alternative.

Après correction par les Chefs de Projet, une nouvelle analyse est lancée. Si le conflit est résolu, l’anomalie passe au statut résolue. Sinon, elle reste ouverte ou en correction jusqu’à résolution.

Ce workflow permet d’améliorer la coordination entre les Chefs de Projet et le Resource Manager, de réduire les conflits d’affectation et d’assurer une meilleure utilisation des ressources de l’entreprise.

---

# 13. Workflow sous forme simple

```text
Chef de Projet crée un projet
        ↓
Chef de Projet importe la prévision V2
        ↓
Le système analyse automatiquement les affectations
        ↓
Le système détecte les anomalies
        ↓
Resource Manager visualise les conflits
        ↓
Resource Manager notifie les Chefs de Projet concernés
        ↓
Chefs de Projet corrigent ou négocient
        ↓
Resource Manager lance une simulation What-if si nécessaire
        ↓
Le système propose des ressources alternatives
        ↓
Nouvelle prévision corrigée / mise à jour
        ↓
Nouvelle analyse automatique
        ↓
Anomalie résolue ou maintenue
```
