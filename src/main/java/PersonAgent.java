import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;


public class PersonAgent extends Agent {

    // variables d agent
    private int nbrTentatives = 0; // compteur
    private boolean reserve = false; // true quand la reservation est bien faite

    @Override
    protected void setup() {
        System.out.println("Agent "+ getLocalName() +" setup");
        addBehaviour(new TentativeReservationBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("Agent "+ getLocalName() +" terminé après"
                + nbrTentatives + " tentatives");
    }

    private class TentativeReservationBehaviour extends Behaviour {

        @Override
        public void action() {


            try {
                Thread.sleep(9000);
            } catch (InterruptedException e) { }

            // chercher restau dans DF
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("restaurant");
            dfd.addServices(sd);

            DFAgentDescription[] restaurants = null;
            try {
                restaurants = DFService.search(myAgent, dfd);
            } catch (FIPAException e) {
                e.printStackTrace();
                block();
                return;
            }

            if (restaurants == null || restaurants.length == 0) {
                System.out.println("Pas de restaurants disponibles pour le moment");
                block(500);
                return;
            }

            // choisir restaurant aleatoirement
            int indexChoix = (int)(Math.random() * restaurants.length);
            AID restaurantChoisi = restaurants[indexChoix].getName();

            nbrTentatives++;

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(restaurantChoisi);
            request.setContent("demande-reservation");
            request.setConversationId("Reserv-" + getLocalName() + "-" + nbrTentatives);
            myAgent.send(request);

            System.out.println("(" + nbrTentatives + " tentatives) ->  "
                    + restaurantChoisi.getLocalName());

            // attendre la reponse du restaurant
            ACLMessage reponse = myAgent.blockingReceive();

            if (reponse != null) {
                if (reponse.getPerformative() == ACLMessage.AGREE) {
                    reserve = true;
                    System.out.println("(" + getLocalName() +
                            ") Reserve chez" + reponse.getSender().getLocalName() +
                            "apres " + nbrTentatives + "tentatives");
                    envoyerResultat();
                } else if (reponse.getPerformative() == ACLMessage.REFUSE) {
                    System.out.println("(" + getLocalName() +
                            ") Refuse par" + reponse.getSender().getLocalName() +
                            "nouvelle tentative ");
                }
            }
        }

        @Override
        public boolean done() {
            return reserve;
        }
    }

    private void envoyerResultat() {
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.addReceiver(new AID("stats", jade.core.AID.ISLOCALNAME));
        inform.setContent(getLocalName() + ":" + nbrTentatives);
        send(inform);
        System.out.println("(" + getLocalName() +
                ") Resultat envoye au StatisticsAgent" + getLocalName() +
                ":" + nbrTentatives);
    }
}
