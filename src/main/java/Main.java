import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {
    // ============================================================
    // Variables de synchronisation partagées avec StatisticsAgent
    // ============================================================
    public static volatile boolean scenarioTermine = false;
    public static final Object verrou = new Object();

    // ============================================================
    // Définition des scénarios (N = nombre de personnes)
    // ============================================================
    static final int[] SCENARIOS_N = {5, 10, 15};

    // Capacités des restaurants pour chaque scénario (M restaurants)
    // On s'assure que Total Ci > 2*N et chaque Ci < N
    static final int[][] SCENARIOS_CAPACITES = {
            {4, 3, 5, 3, 4},              // Scénario 1 : N=5, M=5, Total=19 (>10)
            {4, 3, 5, 3, 4, 4, 3, 5},     // Scénario 2 : N=10, M=8, Total=31 (>20)
            {4, 3, 5, 3, 4, 4, 3, 5, 4, 3} // Scénario 3 : N=15, M=10, Total=38 (>30)
    };

    // Stockage des résultats finaux pour le tableau
    public static double[] moyennesResultats = new double[3];
    public static int[] totauxResultats = new int[3];

    public static void main(String[] args) throws Exception {
        System.out.println("==============================================");
        System.out.println("   DEMARRAGE DE LA SIMULATION MULTI-SCENARIOS ");
        System.out.println("==============================================\n");

        // 1. Initialisation de l'environnement JADE
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true"); // Affiche l'interface JADE
        AgentContainer container = rt.createMainContainer(profile);

        // Petit temps de pause pour laisser le GUI s'ouvrir
        Thread.sleep(2000);

        // 2. Boucle sur les 3 scénarios
        for (int s = 0; s < SCENARIOS_N.length; s++) {
            int N = SCENARIOS_N[s];
            int[] capacites = SCENARIOS_CAPACITES[s];
            int M = capacites.length;

            System.out.println(">>> LANCEMENT SCENARIO " + (s + 1) + " (N=" + N + ", M=" + M + ")");

            scenarioTermine = false;

            // --- ETAPE A : Lancer StatisticsAgent ---
            // On lui passe N et l'index du scénario (s) en arguments
            AgentController stats = container.createNewAgent(
                    "stats",
                    "StatisticsAgent",
                    new Object[]{String.valueOf(N), String.valueOf(s)}
            );
            stats.start();

            // --- ETAPE B : Lancer les RestaurantAgents ---
            for (int i = 0; i < M; i++) {
                AgentController restau = container.createNewAgent(
                        "R" + (i + 1),
                        "RestaurantAgent",
                        new Object[]{String.valueOf(capacites[i])}
                );
                restau.start();
            }

            // Attendre un peu que les restaurants s'inscrivent dans le DF
            Thread.sleep(1000);

            // --- C'EST ICI QU'IL FAUT METTRE LA LONGUE PAUSE ---
            System.out.println("⚠️ PAUSE DE 60s : Activez le Sniffer dans JADE maintenant !");
            Thread.sleep(60000); // 60 secondes pour être tranquille

            // --- ETAPE C : Lancer les PersonAgents ---
            for (int i = 0; i < N; i++) {
                AgentController perso = container.createNewAgent(
                        "P" + (i + 1),
                        "PersonAgent",
                        null
                );
                perso.start();
            }

            // --- ETAPE D : ATTENTE DE FIN DU SCENARIO ---
            synchronized (verrou) {
                while (!scenarioTermine) {
                    System.out.println("Main : En attente des résultats...");
                    verrou.wait();
                }
            }

            System.out.println(">>> SCENARIO " + (s + 1) + " TERMINE.\n");

// ============================================================
            // C'EST ICI : NETTOYAGE DES AGENTS DU SCÉNARIO PRÉCÉDENT
            // ============================================================
            System.out.println("Nettoyage des agents pour le prochain scénario...");

            // On tue les restaurants (R1, R2...)
            for (int i = 1; i <= M; i++) {
                try {
                    container.getAgent("R" + i).kill();
                } catch (Exception e) { /* Déjà supprimé */ }
            }

            // On tue les personnes (P1, P2...)
            for (int i = 1; i <= N; i++) {
                try {
                    container.getAgent("P" + i).kill();
                } catch (Exception e) { /* Déjà terminé */ }
            }

            // Petite pause pour laisser JADE libérer les noms d'agents
            Thread.sleep(2000);

        } // <--- FIN DE LA BOUCLE FOR (s)

        // 3. Affichage du tableau final (en dehors de la boucle)
        afficherTableauRecapitulatif();
    }

    private static void afficherTableauRecapitulatif() {
        System.out.println("\n======= TABLEAU COMPARATIF FINAL =======");
        System.out.println("Scénario | N  | M  | Total Appels | Moyenne");
        System.out.println("----------------------------------------");
        for (int i = 0; i < SCENARIOS_N.length; i++) {
            System.out.printf("   %d     | %2d | %2d |     %3d      |  %.2f %n",
                    (i+1), SCENARIOS_N[i], SCENARIOS_CAPACITES[i].length, totauxResultats[i], moyennesResultats[i]);
        }
        System.out.println("========================================\n");
    }
}