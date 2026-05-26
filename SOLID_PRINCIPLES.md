# Principes SOLID & Clean Code — Authentification JWT

Ce document explique où et comment les principes SOLID et les bonnes pratiques de Clean Code sont appliqués dans le module d'authentification JWT du projet Staff2Staff.

---

## Table des matières
1. [S — Single Responsibility Principle](#s--single-responsibility-principle)
2. [O — Open/Closed Principle](#o--openclosed-principle)
3. [L — Liskov Substitution Principle](#l--liskov-substitution-principle)
4. [I — Interface Segregation Principle](#i--interface-segregation-principle)
5. [D — Dependency Inversion Principle](#d--dependency-inversion-principle)
6. [Design Patterns utilisés](#design-patterns-utilisés)
7. [Principes Clean Code](#principes-clean-code)

---

## S — Single Responsibility Principle

> *Chaque classe doit avoir une seule raison de changer.*

| Classe | Responsabilité unique |
|---|---|
| `AuthController` | Gère uniquement les requêtes HTTP d'authentification (mapping, validation, réponse). Ne contient aucune logique métier. |
| `AuthServiceImpl` | Gère uniquement la logique d'authentification (vérification email/password, génération token). |
| `JwtServiceImpl` | Gère uniquement les opérations JWT (génération, parsing, validation des tokens). |
| `CustomUserDetailsService` | Charge uniquement un utilisateur depuis la BD pour Spring Security. |
| `UserDetailsAdapter` | Adapte uniquement un `User` entity vers `UserDetails`. |
| `JwtAuthenticationFilter` | Filtre uniquement les requêtes HTTP pour extraire et valider le JWT. |
| `SecurityConfig` | Configure uniquement la chaîne de sécurité (filtres, CORS, sessions). |
| `DataSeeder` | Insère uniquement les données initiales au démarrage. |
| `GlobalExceptionHandler` | Centralise uniquement le mapping exception → réponse HTTP. |
| `LoginRequestDTO` | Transporte uniquement les données d'entrée de login. |
| `LoginResponseDTO` | Transporte uniquement les données de sortie après login. |

### Exemple concret

```java
// AuthController — NE fait PAS de logique métier
@PostMapping("/login")
public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    LoginResponseDTO response = authService.login(request);  // délègue au service
    return ResponseEntity.ok(response);
}
```

```java
// AuthServiceImpl — NE gère PAS les tokens directement
String token = jwtService.generateToken(userDetails);  // délègue à JwtService
```

---

## O — Open/Closed Principle

> *Les classes doivent être ouvertes à l'extension, fermées à la modification.*

### Où c'est appliqué :

1. **`GlobalExceptionHandler`** — Pour gérer un nouveau type d'exception, il suffit d'ajouter une nouvelle méthode `@ExceptionHandler` sans modifier les méthodes existantes :

```java
// Ajouter sans modifier le code existant :
@ExceptionHandler(NewBusinessException.class)
public ResponseEntity<Map<String, Object>> handleNewException(NewBusinessException ex) {
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
}
```

2. **`SecurityConfig`** — Pour ajouter de nouvelles règles de sécurité (ex: accès par rôle), on étend la configuration sans modifier le filter chain existant :

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/admin/**").hasRole("RESOURCE_MANAGER")  // ← extension
    .anyRequest().authenticated()
)
```

3. **`JwtService` (interface)** — Si on veut un nouveau type de token (refresh token), on peut créer une nouvelle implémentation sans toucher `JwtServiceImpl`.

---

## L — Liskov Substitution Principle

> *Les objets d'une sous-classe doivent pouvoir remplacer les objets de la classe parente sans altérer le comportement.*

### Où c'est appliqué :

1. **`UserDetailsAdapter` implémente `UserDetails`** — Spring Security attend un `UserDetails`. Notre adapter le respecte intégralement. Tout code utilisant `UserDetails` fonctionne identiquement avec notre adapter.

```java
// Spring Security attend UserDetails — notre adapter est transparent :
UserDetails userDetails = new UserDetailsAdapter(user);
jwtService.isTokenValid(jwt, userDetails);  // fonctionne sans savoir que c'est un adapter
```

2. **`JwtServiceImpl` implémente `JwtService`** — Toute classe qui dépend de `JwtService` peut recevoir n'importe quelle implémentation.

3. **`AuthServiceImpl` implémente `AuthService`** — Le contrôleur fonctionne avec n'importe quelle implémentation de `AuthService`.

---

## I — Interface Segregation Principle

> *Les clients ne doivent pas être forcés de dépendre d'interfaces qu'ils n'utilisent pas.*

### Où c'est appliqué :

1. **`JwtService`** — Interface focalisée avec seulement 3 méthodes liées aux JWT :
   - `generateToken()`, `extractEmail()`, `isTokenValid()`
   - On n'a PAS mis des méthodes d'authentification (login) ici.

2. **`AuthService`** — Interface avec une seule méthode `login()`.
   - On n'a PAS mélangé les opérations de gestion d'utilisateurs ici.

3. **Séparation DTOs** :
   - `LoginRequestDTO` — seulement ce qui est nécessaire pour s'authentifier (email, password)
   - `LoginResponseDTO` — seulement ce qui est retourné au client (token, profil)
   - On n'a PAS créé un DTO monolithique qui fait tout.

```java
// Interface focalisée — seulement les opérations JWT
public interface JwtService {
    String generateToken(UserDetails userDetails);
    String extractEmail(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}
```

---

## D — Dependency Inversion Principle

> *Dépendre des abstractions, pas des implémentations concrètes.*

### Où c'est appliqué :

1. **`AuthController` → `AuthService` (interface)**
   ```java
   // Le controller dépend de l'interface, PAS de AuthServiceImpl
   private final AuthService authService;
   ```

2. **`AuthServiceImpl` → `JwtService` (interface), `PasswordEncoder` (interface), `UserRepository` (interface)**
   ```java
   private final UserRepository userRepository;     // interface Spring Data
   private final PasswordEncoder passwordEncoder;   // interface Spring Security
   private final JwtService jwtService;             // notre interface
   ```

3. **`JwtAuthenticationFilter` → `JwtService` (interface), `UserDetailsService` (interface)**
   ```java
   private final JwtService jwtService;             // interface
   private final UserDetailsService userDetailsService;  // interface Spring Security
   ```

4. **Injection par constructeur** — Toutes les dépendances sont injectées via `@RequiredArgsConstructor` (Lombok), ce qui :
   - Favorise l'immutabilité (champs `final`)
   - Facilite les tests unitaires (mock des interfaces)
   - Rend les dépendances explicites

---

## Design Patterns utilisés

### 1. Adapter Pattern
**`UserDetailsAdapter`** adapte notre entité `User` (domaine métier) vers l'interface `UserDetails` (framework Spring Security).

```
┌──────────┐     ┌───────────────────┐     ┌─────────────┐
│   User   │ ──▶ │ UserDetailsAdapter │ ──▶ │ UserDetails │
│ (Entity) │     │    (Adapter)      │     │ (Interface) │
└──────────┘     └───────────────────┘     └─────────────┘
```

### 2. Filter (Chain of Responsibility)
**`JwtAuthenticationFilter`** s'inscrit dans la chaîne de filtres de Spring Security (`OncePerRequestFilter`). Chaque requête passe par le filtre qui décide d'authentifier ou de passer au suivant.

### 3. Builder Pattern
**`LoginResponseDTO`** utilise le pattern Builder (via Lombok `@Builder`) pour construire les réponses de manière fluide et lisible.

```java
return LoginResponseDTO.builder()
    .token(token)
    .email(user.getEmail())
    .role(user.getRole().name())
    .build();
```

### 4. Strategy Pattern (implicite)
Spring injecte l'implémentation de `PasswordEncoder` (BCrypt). On pourrait changer la stratégie de hashage en modifiant le bean sans toucher au code métier.

---

## Principes Clean Code

### 1. Nommage expressif
- `LoginRequestDTO`, `LoginResponseDTO` — noms qui décrivent exactement le rôle
- `extractEmail()`, `isTokenValid()`, `generateToken()` — verbes d'action clairs
- `CustomUserDetailsService` — indique clairement que c'est notre implémentation personnalisée

### 2. Fonctions courtes et focalisées
Chaque méthode fait une seule chose. Exemple dans `AuthServiceImpl.login()` :
1. Trouver l'utilisateur
2. Vérifier le mot de passe
3. Générer le token
4. Construire la réponse

### 3. Pas de valeurs magiques
- Le secret JWT et l'expiration sont configurés dans `application.properties`
- Les messages d'erreur sont des chaînes descriptives en français

### 4. Gestion d'erreurs propre
- `AuthenticationException` — exception métier personnalisée
- `GlobalExceptionHandler` — centralise les réponses d'erreur
- Pas de `try/catch` dispersés dans les controllers

### 5. Séparation des couches
```
Controller → Service → Repository
     ↓          ↓          ↓
   HTTP     Business      Data
   Layer      Logic      Access
```

### 6. DTOs vs Entités
- Les entités ne sont JAMAIS retournées directement en réponse HTTP
- Le mot de passe hashé n'est JAMAIS inclus dans la réponse
- Les DTOs assurent un contrat API propre et sécurisé

### 7. Immutabilité
- Tous les champs injectés sont `final` (via `@RequiredArgsConstructor`)
- Les DTOs utilisent `@Builder` pour une construction immuable

### 8. Convention de code
- Package `security/` — tout ce qui concerne la sécurité
- Package `service/` — logique métier
- Package `controller/` — endpoints REST
- Package `config/` — configuration Spring
- Package `exception/` — exceptions personnalisées
- Package `DTO/request/` et `DTO/response/` — séparation entrée/sortie


---

# Principes SOLID & Clean Code — Gestion des Prévisions V2

Cette section documente l'application des principes SOLID dans le module de **gestion des prévisions V2**, qui permet à un Chef de Projet d'importer des fichiers Excel de prévision, de consulter l'historique, de télécharger les fichiers et de visualiser les statistiques associées.

L'architecture suit le pattern en couches `Controller → Service → Repository`, en cohérence avec le module d'authentification JWT décrit ci-dessus.

---

## Table des matières
1. [S — Single Responsibility Principle](#s--single-responsibility-principle-prévisions)
2. [O — Open/Closed Principle](#o--openclosed-principle-prévisions)
3. [L — Liskov Substitution Principle](#l--liskov-substitution-principle-prévisions)
4. [I — Interface Segregation Principle](#i--interface-segregation-principle-prévisions)
5. [D — Dependency Inversion Principle](#d--dependency-inversion-principle-prévisions)
6. [Design Patterns utilisés](#design-patterns-utilisés-prévisions)
7. [Principes Clean Code](#principes-clean-code-prévisions)

---

## S — Single Responsibility Principle (Prévisions)

> *Chaque classe doit avoir une seule raison de changer.*

| Classe | Responsabilité unique |
|---|---|
| `PrevisionController` | Gère uniquement les préoccupations HTTP : mapping des routes (`/api/projets/{id}/previsions`, `/api/previsions/{id}/download`, etc.), extraction des paramètres (`@PathVariable`, `@RequestParam`, `MultipartFile`), construction des `ResponseEntity` (codes 201, 200, 204) et propagation de `Authentication`. **Aucune logique métier**. |
| `PrevisionServiceImpl` | Contient exclusivement la logique métier : validation du fichier (extension, taille, contenu vide), parsing Excel via Apache POI, vérification d'ownership, archivage transactionnel, calcul des statistiques (mois couverts, collaborateurs distincts), mapping Entity → DTO. |
| `PrevisionRepository` | Gère uniquement l'accès aux données : requêtes JPA dérivées (`findByProjet`, `findByProjetOrderByDateImportDesc`, `findByProjetAndTypePrevisionAndActiveTrue`). Aucune règle métier. |
| `Prevision` (Entity) | Modélise uniquement les données persistées : champs `id`, `nomFichier`, `typePrevision`, `periodeDebut`, `periodeFin`, `dateImport`, `active`, `fichierData`, et relations `@ManyToOne` vers `User` (importePar) et `Projet`. |
| `PrevisionResponseDTO` | Transporte uniquement les données de sortie d'une prévision (id, nomFichier, type, périodes, statut, importeParNomComplet, projetId, projetNom). Pas de logique. |
| `PrevisionStatsDTO` | Transporte uniquement les statistiques calculées (nombreCollaborateurs, nombreMois, typePrevision, dateImport). |
| `GlobalExceptionHandler` | Centralise uniquement le mapping exception → réponse HTTP, y compris les nouvelles exceptions liées à l'upload (`MaxUploadSizeExceededException`, `MethodArgumentTypeMismatchException`). |

### Exemple concret — Le contrôleur ne contient aucune logique métier

```java
// PrevisionController — délègue intégralement au service
@PostMapping("/projets/{projetId}/previsions")
@PreAuthorize("hasRole('CHEF_PROJET')")
public ResponseEntity<PrevisionResponseDTO> importerPrevision(
        @PathVariable Long projetId,
        @RequestParam("file") MultipartFile file,
        @RequestParam("typePrevision") TypePrevision typePrevision,
        Authentication authentication) {
    PrevisionResponseDTO response = previsionService.importerPrevision(
            projetId, file, typePrevision, authentication);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### Exemple concret — Le service ne touche pas au HTTP

```java
// PrevisionServiceImpl — pure logique métier, délègue l'accès aux données
@Override
@Transactional
public PrevisionResponseDTO importerPrevision(Long projetId, MultipartFile file,
        TypePrevision typePrevision, Authentication authentication) {
    User user = resolveUser(authentication);
    Projet projet = resolveProjetWithOwnershipCheck(projetId, user);
    validateFile(file);                              // règle métier
    ExcelParseResult parseResult = parseExcelFile(file);  // règle métier
    archivePreviousActive(projet, typePrevision);    // règle métier (invariant)
    // ... persistance via previsionRepository.save(...)
}
```

---

## O — Open/Closed Principle (Prévisions)

> *Les classes doivent être ouvertes à l'extension, fermées à la modification.*

### Où c'est appliqué :

1. **`GlobalExceptionHandler`** — Le module Prévisions a introduit deux nouvelles exceptions liées à l'upload de fichier. Elles ont été ajoutées **sans modifier** les `@ExceptionHandler` existants (`AuthenticationException`, `BusinessValidationException`, `ResourceNotFoundException`, etc.) :

   ```java
   // Ajoutés pour le module Prévisions, sans toucher aux handlers existants :

   @ExceptionHandler(MaxUploadSizeExceededException.class)
   public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
           MaxUploadSizeExceededException ex) {
       return buildErrorResponse(HttpStatus.BAD_REQUEST,
               "La taille maximale autorisée est de 10 Mo");
   }

   @ExceptionHandler(MethodArgumentTypeMismatchException.class)
   public ResponseEntity<Map<String, Object>> handleTypeMismatch(
           MethodArgumentTypeMismatchException ex) {
       if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
           return buildErrorResponse(HttpStatus.BAD_REQUEST,
                   "Les valeurs acceptées sont : TRIMESTRIELLE, ANNUELLE");
       }
       return buildErrorResponse(HttpStatus.BAD_REQUEST, "Paramètre invalide");
   }
   ```

   Si demain on ajoute un nouvel endpoint qui peut lever `MultipartException`, il suffira d'ajouter une nouvelle méthode `@ExceptionHandler(MultipartException.class)` sans toucher aux dix handlers déjà présents.

2. **`SecurityConfig`** — Les règles de sécurité du module ont été ajoutées par **extension** de la chaîne de filtres existante, sans réécrire ni modifier les règles précédentes :

   ```java
   .authorizeHttpRequests(auth -> auth
       .requestMatchers("/api/auth/**").permitAll()
       .requestMatchers(HttpMethod.POST, "/api/projets").hasRole("CHEF_PROJET")
       .requestMatchers("/api/projets/*/previsions/**").hasRole("CHEF_PROJET")  // ← extension
       .requestMatchers("/api/previsions/**").hasRole("CHEF_PROJET")            // ← extension
       .anyRequest().authenticated()
   )
   ```

3. **`PrevisionService` (interface)** — Si l'on souhaite plus tard remplacer le parsing Excel local par un parser distant, ou ajouter un mode d'import asynchrone, il suffit de fournir une nouvelle implémentation de l'interface `PrevisionService` sans modifier `PrevisionServiceImpl`.

4. **Stratégie de validation extensible** — La méthode `validateFile()` s'appuie sur des constantes (`MAX_FILE_SIZE`, `ALLOWED_EXTENSIONS`) et des règles isolées. Ajouter une règle (par ex. signature MIME) consiste à ajouter une vérification supplémentaire sans réécrire l'existante.

---

## L — Liskov Substitution Principle (Prévisions)

> *Les objets d'une sous-classe doivent pouvoir remplacer les objets de la classe parente sans altérer le comportement.*

### Où c'est appliqué :

1. **`PrevisionServiceImpl` implémente `PrevisionService`** — Toute classe qui dépend de `PrevisionService` (le contrôleur, les tests) peut recevoir n'importe quelle implémentation respectant le contrat. Les tests unitaires peuvent injecter un mock sans casser le comportement attendu.

   ```java
   // Le contrôleur fonctionne avec n'importe quelle implémentation de PrevisionService
   private final PrevisionService previsionService;
   ```

2. **`PrevisionRepository extends JpaRepository<Prevision, Long>`** — Spring Data fournit une implémentation transparente conforme au contrat de `JpaRepository`. Tout le code qui utilise les méthodes héritées (`save`, `findById`, `saveAll`) fonctionne identiquement avec l'implémentation générée.

3. **Respect des contrats d'exception** — Les méthodes du service respectent strictement les contrats déclarés dans l'interface `PrevisionService` : par exemple, `getPrevisionActive` retourne `Optional<PrevisionResponseDTO>` (vide si aucune prévision active), conformément à la signature, sans lever d'exception inattendue.

---

## I — Interface Segregation Principle (Prévisions)

> *Les clients ne doivent pas être forcés de dépendre d'interfaces qu'ils n'utilisent pas.*

### Où c'est appliqué :

1. **`PrevisionService`** — Interface focalisée exclusivement sur les opérations de prévision :
   - `importerPrevision`, `getHistorique`, `getPrevisionActive`, `telechargerPrevision`, `getStatistiques`
   - Aucune méthode liée à la gestion des projets, des utilisateurs ou de l'authentification n'est mélangée ici.

2. **Séparation des DTOs** :
   - `PrevisionResponseDTO` — données de sortie d'une prévision uniquement (10 champs ciblés, pas de payload binaire).
   - `PrevisionStatsDTO` — uniquement les statistiques calculées (4 champs).
   - Le contenu binaire (`fichierData`) reste interne à l'entité et n'est exposé qu'à travers l'endpoint de download dédié.

3. **`PrevisionRepository`** — Expose uniquement les requêtes utiles au domaine prévision, sans hériter d'opérations qui n'ont pas de sens (suppression en masse, requêtes sur d'autres entités).

```java
// Interface focalisée — uniquement les opérations Prévision
public interface PrevisionService {
    PrevisionResponseDTO importerPrevision(Long projetId, MultipartFile file,
            TypePrevision typePrevision, Authentication authentication);
    List<PrevisionResponseDTO> getHistorique(Long projetId, Authentication authentication);
    Optional<PrevisionResponseDTO> getPrevisionActive(Long projetId, Authentication authentication);
    ResponseEntity<byte[]> telechargerPrevision(Long previsionId, Authentication authentication);
    PrevisionStatsDTO getStatistiques(Long previsionId, Authentication authentication);
}
```

---

## D — Dependency Inversion Principle (Prévisions)

> *Dépendre des abstractions, pas des implémentations concrètes.*

### Où c'est appliqué :

1. **`PrevisionController` → `PrevisionService` (interface)**

   Le contrôleur ne connaît pas `PrevisionServiceImpl`. Il dépend uniquement de l'abstraction :

   ```java
   @RestController
   @RequestMapping("/api")
   @RequiredArgsConstructor
   public class PrevisionController {
       // Dépendance vers l'interface, pas vers l'implémentation
       private final PrevisionService previsionService;
       // ...
   }
   ```

   Cela permet de substituer librement l'implémentation (mock pour les tests, implémentation alternative en production) sans toucher au contrôleur.

2. **`PrevisionServiceImpl` → Repository (interfaces uniquement)**

   Le service métier dépend exclusivement des interfaces Repository, jamais d'une implémentation concrète :

   ```java
   @Service
   @RequiredArgsConstructor
   public class PrevisionServiceImpl implements PrevisionService {

       private final PrevisionRepository previsionRepository;   // interface Spring Data
       private final ProjetRepository projetRepository;         // interface Spring Data
       private final UserRepository userRepository;             // interface Spring Data
       private final AffectationRepository affectationRepository; // interface Spring Data
       // ...
   }
   ```

   Les implémentations sont fournies par Spring Data au runtime via le proxy JPA. Le service n'est couplé qu'au contrat.

3. **Injection par constructeur exclusivement**

   Toutes les dépendances sont déclarées `final` et injectées via `@RequiredArgsConstructor` (Lombok). Aucun champ `@Autowired` n'est utilisé dans le module Prévisions. Cela :
   - Force la déclaration explicite des dépendances.
   - Garantit l'immutabilité des références.
   - Facilite le mock des dépendances dans les tests unitaires (Mockito + constructeur).
   - Conforme au requirement d'architecture **9.6**.

4. **Conformité explicite aux requirements 9.1, 9.2, 9.5, 9.7** :
   - **9.1** : `PrevisionService` est défini comme une interface, `PrevisionServiceImpl` est l'implémentation `@Service`.
   - **9.2** : `PrevisionController` déclare sa dépendance via le type de l'interface uniquement.
   - **9.5** : `PrevisionServiceImpl` ne contient aucune logique HTTP ni appel direct à `EntityManager` ou aux API JDBC ; tout passe par les Repository.
   - **9.7** : Les dépendances vers les Repository sont déclarées via leurs interfaces (`PrevisionRepository`, `ProjetRepository`, `UserRepository`, `AffectationRepository`).

---

## Design Patterns utilisés (Prévisions)

### 1. Builder Pattern
Les DTOs et l'entité `Prevision` utilisent `@Builder` (Lombok) pour une construction lisible et immuable :

```java
Prevision prevision = Prevision.builder()
        .nomFichier(truncateFileName(file.getOriginalFilename()))
        .typePrevision(typePrevision)
        .periodeDebut(parseResult.periodeDebut())
        .periodeFin(parseResult.periodeFin())
        .dateImport(LocalDateTime.now())
        .active(true)
        .importePar(user)
        .projet(projet)
        .fichierData(file.getBytes())
        .build();
```

### 2. Repository Pattern
`PrevisionRepository`, `ProjetRepository`, `UserRepository`, `AffectationRepository` encapsulent l'accès aux données. Le service métier ignore complètement comment les requêtes sont exécutées (JPQL, SQL natif, criteria API).

### 3. Template Method (implicite via Spring Data)
`JpaRepository` fournit le squelette des opérations CRUD. Spring Data génère les implémentations à partir des noms de méthodes (`findByProjetOrderByDateImportDesc`).

### 4. Strategy Pattern (Apache POI)
`WorkbookFactory.create(InputStream)` choisit dynamiquement la stratégie de parsing : `XSSFWorkbook` pour `.xlsx`, `HSSFWorkbook` pour `.xls`. Le service métier reste indépendant du format.

### 5. Specification implicite — Invariant transactionnel
La méthode `archivePreviousActive` + la création de la nouvelle prévision sont enveloppées dans `@Transactional`, garantissant l'invariant "exactement une prévision active par (Projet, TypePrevision)" même en cas d'échec partiel.

---

## Principes Clean Code (Prévisions)

### 1. Nommage expressif (français pour le métier, anglais pour la technique)
- `importerPrevision`, `getHistorique`, `telechargerPrevision`, `getStatistiques` — verbes d'action métier en français, alignés avec le vocabulaire du domaine.
- `resolveUser`, `verifyOwnership`, `validateFile`, `archivePreviousActive`, `mapToResponseDTO` — verbes techniques précis pour les helpers privés.
- `ExcelParseResult` — record interne dont le nom décrit exactement son rôle.

### 2. Fonctions courtes et focalisées
Chaque méthode publique du service suit un workflow clair de 4 à 6 étapes, avec des helpers privés pour chaque responsabilité :

```java
// importerPrevision : 6 étapes lisibles, chacune une intention métier distincte
User user = resolveUser(authentication);
Projet projet = resolveProjetWithOwnershipCheck(projetId, user);
validateFile(file);
ExcelParseResult parseResult = parseExcelFile(file);
archivePreviousActive(projet, typePrevision);
// + persistance et mapping
```

### 3. Pas de valeurs magiques
- `MAX_FILE_SIZE = 10L * 1024 * 1024` — constante explicite, pas de `10485760` éparpillé dans le code.
- `ALLOWED_EXTENSIONS = Set.of("xlsx", "xls")` — liste centralisée des extensions acceptées.
- Limites multipart configurées dans `application.properties` (`spring.servlet.multipart.max-file-size=10MB`).

### 4. Gestion d'erreurs propre
- Exceptions métier typées : `BusinessValidationException`, `ResourceNotFoundException`, `AccessDeniedException`.
- Mapping centralisé dans `GlobalExceptionHandler` (HTTP 400, 403, 404 selon le cas).
- Aucun `try/catch` dispersé dans le contrôleur : les exceptions remontent et sont traitées globalement.

### 5. Séparation stricte des couches
```
PrevisionController  →  PrevisionService  →  PrevisionRepository
       ↓                       ↓                      ↓
     HTTP                  Logique métier         Accès données
   (mapping,             (validation,            (requêtes JPA)
    DTOs)                 archivage,
                          parsing POI)
```

### 6. DTOs vs Entités
- L'entité `Prevision` n'est **jamais** sérialisée directement dans une réponse HTTP.
- `PrevisionResponseDTO` expose uniquement ce qui est utile au client (nom complet de l'utilisateur, nom du projet) sans fuite de données sensibles ou du contenu binaire (`fichierData`).
- Le contenu binaire est exposé uniquement via l'endpoint `/download` dédié, avec les headers HTTP appropriés.

### 7. Immutabilité et injection
- Toutes les dépendances du contrôleur et du service sont `final`.
- Construction via `@RequiredArgsConstructor` (Lombok) — pas de setters, pas de `@Autowired` sur champs.
- Les DTOs utilisent `@Builder` pour une construction immuable.

### 8. Traçabilité et invariants documentés
Les invariants critiques (unicité de la prévision active, atomicité de l'archivage) sont explicitement documentés dans le design et garantis par `@Transactional`. Les propriétés universelles correspondantes sont vérifiées par des tests property-based (jqwik).

