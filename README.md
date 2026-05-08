## 🤖 SMA – Problème d'Allocation de Ressources (Comportement Aléatoire)

Un système multi-agents (SMA) simulant un problème classique d'allocation de ressources,
implémenté sur la plateforme **JADE 4.6.0** en Java et conforme aux spécifications FIPA.

### 📌 Problème modélisé
N personnes cherchent à réserver une place dans l'un des M restaurants
à capacité limitée. Chaque agent choisit un restaurant **au hasard**
et réessaie automatiquement en cas de refus — jusqu'à obtenir une réservation.

### 🧩 Architecture des agents
- **RestaurantAgent** — gère les demandes via un `CyclicBehaviour`, répond AGREE ou REFUSE
- **PersonAgent** — choisit aléatoirement un restaurant, relance si refus (`done() = false`)
- **StatisticsAgent** — collecte les rapports de tentatives et calcule la moyenne finale

### 🛠️ Technologies & Méthodologies
- **Plateforme** : JADE 4.6.0 (Java Agent DEvelopment Framework)
- **Langage** : Java
- **Conception** : AUML · GAIA · AALAADIN (AGR) · Méthode des Voyelles
- **Communication** : Messages ACL FIPA (REQUEST / AGREE / REFUSE / INFORM)
- **Annuaire** : Directory Facilitator (DF) de JADE

### 📊 Scénarios testés
| Scénario | N (personnes) | M (restaurants) |
|----------|--------------|-----------------|
| 1        | 5            | 5               |
| 2        | 10           | 8               |
| 3        | 15           | 10              |

### 💡 Question théorique explorée
> *Que se passe-t-il si chaque agent sait que tous les autres raisonnent comme lui ?*
> → Paradoxe de coordination (Problème du Bar d'El Farol, Arthur 1994).

---
📚 Réalisé dans le cadre du module **Systèmes Multi-Agents** — FST Tanger, 2025–2026
