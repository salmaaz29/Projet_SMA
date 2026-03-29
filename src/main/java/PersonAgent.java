import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import static jade.lang.acl.ACLParserConstants.AID;

public class PersonAgent extends Agent {

    // variables d agent
    private int nbrTentatives ; // compteur
    private boolean reserve = false; // true quand la reservation est bien faite

    // setup() va etre appele auto au demarrage de l agent

    @Override
    protected void setup() {

        System.out.println("Agent "+ getLocalName() +" setup");
        // il faut lancer le behaviour qui va gerer toute la logique de reservation
        addBehaviour(new TentativeReservationBehaviour());

    }

    // appele auto lorsque agent s arrete
    @Override
    protected void takeDown() {
        System.out.println("Agent "+ getLocalName() +" terminé après"
            + nbrTentatives + " tentatives");
    }



    // behavipour simple :
    // action() est executee en boucle tant que done() false
    // done() return true seulement quand reserve = true

    private class TentativeReservationBehaviour extends Behaviour  {


        @Override
        public void action() {

            // chercher restau dans DF
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("restaurant");
            dfd.addServices(sd);

            // initialiser liste des agents qui offre service restaurant
            DFAgentDescription[] restaurants = null;
            try{

                restaurants = DFService.search(myAgent, dfd);

            } catch (FIPAException e) {
                e.printStackTrace();
                block(); // attendre avant de ressayer
                return;
            }

            // verifier qu il y a des restau dispo

            if(restaurants == null || restaurants.length == 0){
                System.out.println("Pas de restaurants disponibles pour le moment");
                block(500);
                return;

            }

            // choisir restaurant aleatoirement
            // Math.random()  renvoie un double entre 0 et 1   on le multiplie par nbre des restaus dispo

            // l’index choisi sera toujours un nombre entier entre 0 et restaurants.length - 1
            int indexChoix = (int)(Math.random()*restaurants.length);
            AID restaurantChoisi = restaurants[indexChoix].getName();
            // compter la tentative et envoyer REQUEST
            nbrTentatives++; // chaque envoie = un coup de fil

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(restaurantChoisi);
            request.setContent("demande-reservation");
            // identifier la concersation si plusieurs agents communique en meme temps
            request.setConversationId("Reserv-" + getLocalName() + "-" + nbrTentatives);
            myAgent.send(request);

            System.out.println("(" + nbrTentatives + " tentatives) ->  " + restaurantChoisi.getLocalName());


            // attendre la reponse du restaurant
            // blockingReceive() suspend ce behaviour jusqu a la reception
            ACLMessage reponse = myAgent.blockingReceive();

            if(reponse != null){
                if(reponse.getPerformative() == ACLMessage.AGREE){
                    // place accordee au agent bien reserve
                    reserve = true;
                    System.out.println("(" + getLocalName() +
                            ") Reserve chez" + reponse.getSender().getLocalName() +
                            "apres " + nbrTentatives+  "tentatives");
                    // informer l autre agent statistque
                    envoyerResultat(); // nomagent:nombretentatives
                } else if(reponse.getPerformative() == ACLMessage.REFUSE){
                    // REFUS  RESERVE RESTE FALSE   done() -> false
                    // action() sera appele auto = nouvelle tentative
                    System.out.println("(" + getLocalName() +
                            ") Refuse par" + reponse.getSender().getLocalName() +
                            "nouvelle tentative ");
                }

            }

        }


        // controle la duree du behaviour
        // false → action() est rappelée (nouvelle tentative)
        // true  → behaviour retiré, agent continue avec takeDown()
        @Override
        public boolean done() {
            return reserve;
        }

    }

    //  methode pour envoyer les resultat au StatisticsAgent
    private void envoyerResultat() {

        // chercher l agent statistique par son nom local "stats" --  doit correspondre au nom donner lors du lancement
        //java jade.Boot  stats:StatisticsAgent
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.addReceiver(new AID("stats", jade.core.AID.ISLOCALNAME));
        inform.setContent(getLocalName() + ":" + nbrTentatives);
        send(inform);
        System.out.println("(" + getLocalName() +
                ") Resultat envoye au StatisticsAgent" + getLocalName() +
                ":" + nbrTentatives);
    }


}
