import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class StatisticsAgent extends Agent {

    // variables de l agent
    // N = nombre des PersonAgent attendus -> doit correspondre au nombre de PersonAgent lances
    // le mettre comme argument lorsque on veut lancer   stats:StatisticsAgent(3)
    private int N = 0 ;

    private int agentsTermines = 0; // combien d agents qui ont deja repondus
    private int TotalTentatives = 0; // somme de toutes les tentatives

    // tableau ou on stocke le detail de chaque agent lorsque on recupere leur infos
    private String[] resultAgent ;

    @Override
    protected void setup() {
        // recuperer l argumment n de nombre des PersonAgent
        Object[] args = getArguments();
        if(args != null && args.length > 0){
            N = Integer.parseInt(args[0].toString());
        }else {
            // valeur par defaut si on oublie l argument
            N = 3;
            System.out.println("Utilisation du nombre par defaut du lancement des PersonAgent qui est : " + N);
        }

        // initialiser tableau des resultats
        resultAgent = new String[N];
        System.out.println("StatisticsAgent est demarre  attend les agents lances de nbre : " + N);

        // Lancer le behaviour de collecte
        // Il tournera en boucle jusqu'à avoir reçu N résultats
        addBehaviour(new CollecteBehaviour());
    }


    @Override
    protected void takeDown() {
        System.out.println("[StatisticsAgent] arrêté");

    }


    //  BEHAVIOUR 1 : CyclicBehaviour — collecte les INFORM
    //  → Tourne en boucle infinie
    //  → À chaque message INFORM reçu, enregistre le résultat
    //  → Quand tous les N agents ont répondu → déclenche le OneShot
    private class CollecteBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            // Essayer de lire un message dans la boîte de réception
            ACLMessage msg = receive();

            if (msg != null) {
                // verifier que cest message INFORM
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    // extraire contenu du message

                    String contenu = msg.getContent();
                    String[] parties = contenu.split(":");
                    if(parties.length == 2){
                        String nomAgent = parties[0];
                        int Tentatives = Integer.parseInt(parties[1]);

                        // accumuler les donnees
                        TotalTentatives += Tentatives;
                        resultAgent[agentsTermines] = nomAgent + "->" + "Tentatives: " + Tentatives;
                        agentsTermines++;

                        System.out.println("[StatisticsAgent] reçu de "
                                + nomAgent + " : " + Tentatives
                                + " tentative(s)  [" + agentsTermines
                                + "/" + N + "]");

                        // ── Condition de déclenchement du OneShotBehaviour ──
                        // Quand TOUS les PersonAgents ont envoyé leur résultat
                        // on lance le behaviour d'affichage final

                        if (agentsTermines >= N) {
                            System.out.println("[StatisticsAgent] "
                                    + "tous les agents ont répondu → calcul...");
                            addBehaviour(new AffichageStatsBehaviour());
                            // Note : le CyclicBehaviour continue de tourner
                            // mais il ne recevra plus de messages → block()
                        }

                    } else {
                        System.out.println("[StatisticsAgent] "
                                + "format de message invalide : " + contenu);
                    }
                }

            } else {
                // Pas de message en attente → suspendre ce behaviour
                // Il se réveillera automatiquement quand un message arrivera
                block();
            }
        }

    }

    //  BEHAVIOUR 2 : OneShotBehaviour — affiche les stats finales
    //  → Déclenché UNE SEULE FOIS par CollecteBehaviour
    //  → Calcule la moyenne et affiche tout
    //  → done() retourne toujours true → retiré immédiatement
    private class AffichageStatsBehaviour extends OneShotBehaviour {


        public void action() {
            // Cette méthode est appelée UNE SEULE FOIS puis le behaviour
            // est automatiquement retiré (done() = true en interne)

            double moyenne = (double) TotalTentatives / N;

            // ── Affichage des résultats ───────────────────────
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║         RÉSULTATS DE LA SIMULATION        ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  Comportement      : ALÉATOIRE            ║");
            System.out.println("║  N (personnes)     : " + N);
            System.out.println("║─────────────────────────────────────────║");
            System.out.println("║  Résultats par agent :                    ║");

            // Afficher le détail de chaque agent
            for (int i = 0; i < agentsTermines; i++) {
                System.out.println("║    " + resultAgent[i]);
            }

            System.out.println("║─────────────────────────────────────────║");
            System.out.println("║  Total tentatives  : " + TotalTentatives);
            System.out.println("║  Moyenne / agent   : "
                    + String.format("%.2f", moyenne));
            System.out.println("╚══════════════════════════════════════════╝\n");

            // Arrêter proprement le StatisticsAgent
            myAgent.doDelete();
        }

    }
}
