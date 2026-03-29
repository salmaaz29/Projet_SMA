import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {

    // ============================================================
    // PARAMÈTRES DE LA SIMULATION — modifiez ici uniquement
    // ============================================================
    static final int N = 5;                          // nombre de personnes
    static final int[] CAPACITES = {4, 3, 5, 3, 4}; // capacité de chaque restaurant
    // ⚠️ Règle : somme(CAPACITES) doit être > 2*N
    // ici : 4+3+5+3+4 = 19 > 10 ✅
    // ⚠️ Règle : chaque Ci doit être < N
    // ici : max = 5 = N → à ajuster si N change

    public static void main(String[] args) throws Exception {

        int M = CAPACITES.length; // nombre de restaurants calculé automatiquement

        // Vérification des règles du problème
        int capaciteTotale = 0;
        for (int c : CAPACITES) capaciteTotale += c;

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     SIMULATION — Projet SMA          ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  N (personnes)       : " + N);
        System.out.println("║  M (restaurants)     : " + M);
        System.out.println("║  Capacité totale     : " + capaciteTotale);
        System.out.println("║  2*N                 : " + (2 * N));
        System.out.println("║  Règle capacité > 2N : "
                + (capaciteTotale > 2 * N ? "✅ OK" : "❌ VIOLATION !"));
        System.out.println("╚══════════════════════════════════════╝");

        // ============================================================
        // 1. Démarrer JADE
        // ============================================================
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true"); // fenêtre graphique JADE
        AgentContainer container = rt.createMainContainer(profile);

        // ============================================================
        // 2. Lancer StatisticsAgent EN PREMIER
        //    → il doit être prêt avant que les PersonAgents finissent
        //    → argument : N (nombre de personnes à attendre)
        // ============================================================
        AgentController stats = container.createNewAgent(
                "stats",                          // nom local → "stats" OBLIGATOIRE
                "StatisticsAgent",                // classe Java
                new Object[]{String.valueOf(N)}   // argument : N
        );
        stats.start();
        System.out.println("▶ StatisticsAgent lancé");
        Thread.sleep(500); // laisser le temps de démarrer

        // ============================================================
        // 3. Lancer les RestaurantAgents
        //    → doivent s'inscrire au DF avant les PersonAgents
        // ============================================================
        for (int i = 0; i < M; i++) {
            String nom = "r" + (i + 1); // r1, r2, r3...
            AgentController restaurant = container.createNewAgent(
                    nom,
                    "RestaurantAgent",
                    new Object[]{String.valueOf(CAPACITES[i])}
            );
            restaurant.start();
            System.out.println("▶ " + nom + " lancé | Capacité : " + CAPACITES[i]);
        }
        Thread.sleep(1000); // laisser le temps aux restaurants de s'inscrire au DF

        // ============================================================
        // 4. Lancer les PersonAgents EN DERNIER
        //    → le DF est maintenant peuplé de restaurants
        // ============================================================
        for (int i = 0; i < N; i++) {
            String nom = "p" + (i + 1); // p1, p2, p3...
            AgentController personne = container.createNewAgent(
                    nom,
                    "PersonAgent",
                    null // pas d'argument pour PersonAgent
            );
            personne.start();
            System.out.println("▶ " + nom + " lancé");
        }

        System.out.println("\n🚀 Simulation démarrée !");
        System.out.println("📊 Attendez les résultats du StatisticsAgent...\n");
    }
}
