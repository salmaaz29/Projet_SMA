import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class StatisticsAgent extends Agent {

    private int N = 0;
    private int agentsTermines = 0;
    private int TotalTentatives = 0;
    private String[] resultAgent;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            N = Integer.parseInt(args[0].toString());
        } else {
            N = 3;
            System.out.println("Utilisation du nombre par defaut : " + N);
        }

        resultAgent = new String[N];
        System.out.println("StatisticsAgent est demarre  attend les agents lances de nbre : " + N);

        addBehaviour(new CollecteBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("[StatisticsAgent] arrêté");
    }

    //  BEHAVIOUR 1 : CyclicBehaviour — collecte les INFORM
    private class CollecteBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = receive();

            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    String contenu = msg.getContent();
                    String[] parties = contenu.split(":");
                    if (parties.length == 2) {
                        String nomAgent = parties[0];
                        int Tentatives = Integer.parseInt(parties[1]);

                        TotalTentatives += Tentatives;
                        resultAgent[agentsTermines] = nomAgent + "->" + "Tentatives: " + Tentatives;
                        agentsTermines++;

                        System.out.println("[StatisticsAgent] reçu de "
                                + nomAgent + " : " + Tentatives
                                + " tentative(s)  [" + agentsTermines
                                + "/" + N + "]");

                        if (agentsTermines >= N) {
                            System.out.println("[StatisticsAgent] "
                                    + "tous les agents ont répondu → calcul...");
                            addBehaviour(new AffichageStatsBehaviour());
                        }

                    } else {
                        System.out.println("[StatisticsAgent] format invalide : " + contenu);
                    }
                }
            } else {
                block();
            }
        }
    }

    //  BEHAVIOUR 2 : OneShotBehaviour — affiche les stats finales
    private class AffichageStatsBehaviour extends OneShotBehaviour {

        public void action() {
            double moyenne = (double) TotalTentatives / N;

            // Récupérer l'index du scénario
            int scenarioIndex = 0;
            Object[] args = myAgent.getArguments();
            if (args != null && args.length > 1) {
                scenarioIndex = Integer.parseInt(args[1].toString());
            }

            // Sauvegarder dans Main
            Main.totauxResultats[scenarioIndex] = TotalTentatives;
            Main.moyennesResultats[scenarioIndex] = moyenne;

            // ← AFFICHAGE DÉTAILLÉ REMIS
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║      RÉSULTATS SCÉNARIO " + (scenarioIndex + 1) + "               ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  Comportement      : ALÉATOIRE            ║");
            System.out.println("║  N (personnes)     : " + N);
            System.out.println("║─────────────────────────────────────────║");
            System.out.println("║  Résultats par agent :                    ║");
            for (int i = 0; i < agentsTermines; i++) {
                System.out.println("║    " + resultAgent[i]);
            }
            System.out.println("║─────────────────────────────────────────║");
            System.out.println("║  Total tentatives  : " + TotalTentatives);
            System.out.println("║  Moyenne / agent   : "
                    + String.format("%.2f", moyenne));
            System.out.println("╚══════════════════════════════════════════╝");

            // Signal de réveil pour Main
            Main.scenarioTermine = true;
            synchronized (Main.verrou) {
                Main.verrou.notifyAll();
            }

            myAgent.doDelete();
        }
    }
}
