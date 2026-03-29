
## Ce que j'ai fait — mes 2 fichiers

### `PersonAgent.java`

Simule une **personne** qui cherche une place dans un restaurant.

**Logique interne :**
- Dans `setup()` → lance un `Behaviour` simple
- Dans `action()` du Behaviour :
    1. Cherche tous les restaurants dans le **DF** via `DFService.search("restaurant")`
    2. Choisit **aléatoirement** un restaurant avec `Math.random()`
    3. Incrémente `nombreTentatives` (= le "coup de fil")
    4. Envoie un message `ACLMessage.REQUEST` au restaurant choisi
    5. Attend la réponse avec `blockingReceive()`
    6. Si `CONFIRM` → met `reserve = true`, envoie `INFORM` au StatisticsAgent, `done()` retourne `true` → s'arrête
    7. Si `REFUSE` → `done()` retourne `false` → `action()` est rappelée automatiquement → nouvelle tentative
- Dans `takeDown()` → affiche le nombre de tentatives final

**Ce dont il a besoin pour fonctionner :**
- Des `RestaurantAgent` inscrits dans le DF avec le type `"restaurant"`
- Un `StatisticsAgent` avec le nom local `"stats"` pour recevoir les résultats

**Format du message envoyé au StatisticsAgent :**
```
performatif : ACLMessage.INFORM
contenu     : "nomAgent:nombreTentatives"
exemple     : "p1:3"   (l'agent p1 a mis 3 tentatives)
```

---

### `StatisticsAgent.java`

Collecte les résultats de **tous** les PersonAgents et affiche les statistiques finales.

**Logique interne :**
- Dans `setup()` → lit `N` depuis les arguments, lance un `CyclicBehaviour`
- `CyclicBehaviour` tourne en boucle :
    - Reçoit les messages `INFORM` des PersonAgents
    - Parse le contenu `"nomAgent:tentatives"`
    - Accumule `totalTentatives`
    - Quand `agentsTermines >= N` → déclenche le `OneShotBehaviour`
- `OneShotBehaviour` s'exécute **une seule fois** :
    - Calcule `moyenne = totalTentatives / N`
    - Affiche le tableau de résultats complet
    - Appelle `doDelete()` pour s'arrêter proprement

---

## Ce que tu dois faire — `RestaurantAgent.java`

### Comportement attendu

Ton agent doit :

1. **Dans `setup()`** :
    - Lire sa capacité `Ci` depuis les arguments : `getArguments()[0]`
    - Initialiser `placesDisponibles = Ci`
    - **S'inscrire dans le DF** avec le type exactement égal à `"restaurant"` ← CRITIQUE
    - Lancer un `CyclicBehaviour`

2. **Dans le `CyclicBehaviour`** :
    - Recevoir les messages `ACLMessage.REQUEST`
    - Si `placesDisponibles > 0` → décrémenter et répondre `ACLMessage.CONFIRM`
    - Si `placesDisponibles = 0` → répondre `ACLMessage.REFUSE`
    - Utiliser `msg.createReply()` pour créer la réponse (ça remplit automatiquement le destinataire)

3. **Dans `takeDown()`** :
    - Se désinscrire du DF : `DFService.deregister(this)`

### Point CRITIQUE — inscription au DF

Le type du service **doit être exactement** `"restaurant"` (minuscule, sans espace) :


ServiceDescription sd = new ServiceDescription();
sd.setType("restaurant");   // ← doit correspondre exactement à ce que cherche PersonAgent


Si ce n'est pas exactement `"restaurant"`, `PersonAgent` ne trouvera aucun restaurant
et tournera en boucle infinie sans jamais réserver.

---

## Comment compiler et lancer

### Compilation (les 2 ou les 3 fichiers ensemble)

```bash
# Compiler tous les fichiers Java du dossier
javac -cp jade.jar *.java
```

### Lancement complet (quand les 2 fichiers RestaurantAgent et mes fichiers sont prêts)

```bash
java -cp jade.jar:. jade.Boot -gui \
  r1:RestaurantAgent(5) \
  r2:RestaurantAgent(3) \
  r3:RestaurantAgent(4) \
  p1:PersonAgent \
  p2:PersonAgent \
  p3:PersonAgent \
  stats:StatisticsAgent(3)
```

> **Sur Windows**, remplacer `:` par `;` dans le classpath :
> ```bash
> java -cp jade.jar;. jade.Boot -gui ...
> ```

### Paramètres importants

| Agent | Argument | Exemple | Signification |
|---|---|---|---|
| `RestaurantAgent` | capacité Ci | `RestaurantAgent(5)` | 5 places disponibles |
| `PersonAgent` | aucun | `PersonAgent` | pas d'argument nécessaire |
| `StatisticsAgent` | N (nb personnes) | `StatisticsAgent(3)` | attend 3 résultats |

> **Règle importante :** le chiffre passé à `StatisticsAgent` doit être **égal**
> au nombre de `PersonAgent` lancés. Si tu lances `p1 p2 p3` → passe `3`.
> Si tu lances `p1 p2 p3 p4 p5` → passe `5`.
> Sinon le StatisticsAgent n'affichera jamais les résultats (il attend indéfiniment).

---

## Résultat attendu dans la console

```
=== [StatisticsAgent] démarré — attend 3 agent(s) ===
=== [p1] démarré ===
=== [p2] démarré ===
=== [p3] démarré ===
[p1] tentative 1 → r2
[p2] tentative 1 → r2
[p3] tentative 1 → r1
[p2] RÉSERVÉ chez r2 après 1 tentative(s)
[p1] REFUSÉ par r2 → nouvelle tentative...
[p1] tentative 2 → r3
[p1] RÉSERVÉ chez r3 après 2 tentative(s)
[p3] RÉSERVÉ chez r1 après 1 tentative(s)
[StatisticsAgent] reçu de p2 : 1 tentative(s)  [1/3]
[StatisticsAgent] reçu de p1 : 2 tentative(s)  [2/3]
[StatisticsAgent] reçu de p3 : 1 tentative(s)  [3/3]

╔══════════════════════════════════════════╗
║         RÉSULTATS DE LA SIMULATION        ║
╠══════════════════════════════════════════╣
║  Comportement      : ALÉATOIRE            ║
║  N (personnes)     : 3
║─────────────────────────────────────────║
║  Résultats par agent :
║    p2 → 1 tentative(s)
║    p1 → 2 tentative(s)
║    p3 → 1 tentative(s)
║─────────────────────────────────────────║
║  Total tentatives  : 4
║  Moyenne / agent   : 1.33
╚══════════════════════════════════════════╝
```

---

## Messages ACL échangés — récapitulatif

| De | Vers | Performatif | Contenu | Quand |
|---|---|---|---|---|
| `PersonAgent` | `RestaurantAgent` | `REQUEST` | `"demande-reservation"` | à chaque tentative |
| `RestaurantAgent` | `PersonAgent` | `CONFIRM` | `"place-reservee"` | si place disponible |
| `RestaurantAgent` | `PersonAgent` | `REFUSE` | `"restaurant-plein"` | si complet |
| `PersonAgent` | `StatisticsAgent` | `INFORM` | `"p1:3"` | quand réservation réussie |

---