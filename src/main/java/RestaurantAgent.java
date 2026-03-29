import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RestaurantAgent extends Agent {

    private int capaciteMax;
    private int placesDisponibles;
    private int nombreReservations = 0;

    @Override
    protected void setup() {

        // Vérifier que la capacité est bien passée en argument
        Object[] args = getArguments();
        if (args == null || args.length == 0) {
            System.out.println("❌ [" + getLocalName() + "] ERREUR : capacité manquante !");
            doDelete();
            return;
        }

        capaciteMax       = Integer.parseInt((String) args[0]);
        placesDisponibles = capaciteMax;

        System.out.println("=== [" + getLocalName() + "] démarré | Capacité : "
                + capaciteMax + " ===");

        // S'inscrire dans le DF avec le type "restaurant"
        // ⚠️ CRITIQUE : doit correspondre exactement à ce que cherche PersonAgent
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("restaurant");   // ← exactement "restaurant"
            sd.setName(getLocalName());
            dfd.addServices(sd);

            DFService.register(this, dfd);
            System.out.println("[" + getLocalName() + "] inscrit dans le DF ✅");

        } catch (Exception e) {
            System.out.println("❌ [" + getLocalName() + "] Erreur inscription DF : "
                    + e.getMessage());
        }

        // Lancer le CyclicBehaviour → écoute en permanence
        addBehaviour(new ReservationBehaviour());
    }

    @Override
    protected void takeDown() {
        // Se désinscrire du DF proprement
        try {
            DFService.deregister(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[" + getLocalName() + "] arrêté | "
                + "Réservations : " + nombreReservations
                + "/" + capaciteMax);
    }

    // ============================================================
    // CyclicBehaviour — tourne indéfiniment
    // Reçoit REQUEST → répond AGREE (place dispo) ou REFUSE (plein)
    // ============================================================
    private class ReservationBehaviour extends CyclicBehaviour {

        @Override
        public void action() {

            // Filtre : on attend uniquement les REQUEST
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                ACLMessage reponse = msg.createReply();

                if (placesDisponibles > 0) {
                    // ✅ Place disponible → AGREE
                    placesDisponibles--;
                    nombreReservations++;
                    reponse.setPerformative(ACLMessage.AGREE);
                    reponse.setContent("place-reservee");

                    System.out.println("[" + myAgent.getLocalName() + "] → AGREE pour "
                            + msg.getSender().getLocalName()
                            + " | Places restantes : " + placesDisponibles);

                } else {
                    // ❌ Complet → REFUSE
                    reponse.setPerformative(ACLMessage.REFUSE);
                    reponse.setContent("restaurant-plein");

                    System.out.println("[" + myAgent.getLocalName() + "] → REFUSE pour "
                            + msg.getSender().getLocalName()
                            + " | COMPLET !");
                }

                myAgent.send(reponse);

            } else {
                // Pas de message → on attend le prochain
                block();
            }
            // done() non redéfini → false par défaut → boucle infinie ♾️
        }
    }
}
