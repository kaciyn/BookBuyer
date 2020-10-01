import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class BookSellerAgent extends Agent
{
    // The catalogue of books for sale (maps the title of a book to its price)
    private Hashtable catalogue;
    
    private BookSellerGui myGui;
    
    protected void setup() {
        
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        catalogue = new Hashtable();
        
        myGui = new BookSellerGui(this);
        myGui.showGui();
        
        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
        
    }
    
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        myGui.dispose();
        System.out.println("Seller-agent " + getAID().getName() + " terminating.");
    }
    
    public void updateCatalogue(final String title, final int price) {
        addBehaviour(new OneShotBehaviour()
        {
            @Override
            public void action() {
                catalogue.put(title, Integer.valueOf(price));
                
            }
        });
    }
    
    /**
     * Inner class OfferRequestsServer.
     * This is the behaviour used by Book-seller agents to serve incoming requests for offer from buyer agents.
     * If the requested book is in the local catalogue the seller agent replies with a PROPOSE message specifying the price. Otherwise a REFUSE message is sent back.
     */
    private class OfferRequestsServer extends CyclicBehaviour
    {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
// Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.get(title);
                if (price != null) {
// The requested book is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                }
                else {
                    block();
// The requested book is NOT available for sale. reply.setPerformative(ACLMessage.REFUSE); reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
        }
    }// End of inner class
    
    private class PurchaseOrdersServer extends CyclicBehaviour
    {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
// ACCEPT_PROPOSAL Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(title + " sold to agent " + msg.getSender().getName());
                }
                else {
// The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    } // End of inner class OfferRequestsServer
}
