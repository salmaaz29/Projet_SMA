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
            // 1. Calcul des stats
            double moyenne = (double) TotalTentatives / N;

            // 2. RECUPERER L'INDEX DU SCÉNARIO
            // On récupère le 2ème argument passé au lancement : c'est le numéro du scénario (0, 1 ou 2)
            int scenarioIndex = 0;
            Object[] args = myAgent.getArguments();
            if (args != null && args.length > 1) {
                scenarioIndex = Integer.parseInt(args[1].toString());
            }

            // 3. SAUVEGARDER DANS LE MAIN
            // On écrit directement les résultats dans les tableaux statiques de la classe Main
            Main.totauxResultats[scenarioIndex] = TotalTentatives;
            Main.moyennesResultats[scenarioIndex] = moyenne;

            // 4. AFFICHAGE CONSOLE (Optionnel, pour le debug)
            System.out.println("   [Stats] Scénario " + (scenarioIndex + 1) + " enregistré.");

            // 5. LE SIGNAL DE RÉVEIL (Très important)
            Main.scenarioTermine = true; // On change le drapeau
            synchronized (Main.verrou) {
                Main.verrou.notifyAll(); // On réveille le Main qui attendait avec wait()
            }

            // 6. FIN DE L'AGENT
            myAgent.doDelete();
        }

    }
}
