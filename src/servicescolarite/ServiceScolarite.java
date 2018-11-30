package servicescolarite;

import donnes.preconvention.PreConvention;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Le service de la scolarité de l’université est en charge de la gestion des inscriptions des
 * étudiants. Lui seul est habilité pour certifier qu’un étudiant, caractérisé par son identité et 
 * son numéro d’étudiant, est bien inscrit à l’université pour une formation diplômante particulière 
 * (un stage s’opère dans le cadre d’une formation diplômante). À chaque formation de niveau Licence
 * ou Master est associé un code et un intitulé (ex. : EIMIBE – M2 ingénierie de la transformation
 * numérique, EIMIAE – M2 ingénierie des données et protection). Chaque formation dépend
 * d’un département d’enseignement disciplinaire (exemple : EIMIBE et EIMIAE dépendent du
 * département « Informatique »).
 * @author marieroca
 */
public class ServiceScolarite {
    private HashMap<Long, PreConvention> conv;
    private HashMap<Long, PreConvention> convTraitees;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
    }

    public ServiceScolarite(HashMap<Long, PreConvention> conv, HashMap<Long, PreConvention> convTraitees) {
        if (conv == null){
                this.conv = new HashMap();
            }else{
                this.conv = conv;
            }
            if (convTraitees == null){
                this.convTraitees = new HashMap();
            }else{
                this.convTraitees = convTraitees;
            }
        try {
            this.recevoir();
        } catch (NamingException ex) {
            Logger.getLogger(ServiceScolarite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public HashMap<Long, PreConvention> getConv() {
        return conv;
    }

    public void setConv(HashMap<Long, PreConvention> conv) {
        this.conv = conv;
    }

    public HashMap<Long, PreConvention> getConvTraitees() {
        return convTraitees;
    }

    public void setConvTraitees(HashMap<Long, PreConvention> convTraitees) {
        this.convTraitees = convTraitees;
    }
    
    public void recevoir() throws NamingException {
        //System.setProperty
        System.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
        System.setProperty("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
        System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
        InitialContext context = new InitialContext();
        
        ConnectionFactory factory = null;
        Connection connection = null;
        String factoryName = "jms/__defaultConnectionFactory";
        String destName = "PreConvention";
        Destination dest = null;
        //nombre de messages que l'on reçoit
        int count = 10;
        Session session = null;
        MessageConsumer receiver = null;
        
        //Toutes les connections sont gérées par le serveur 
        try {
            // look up the ConnectionFactory
            factory = (ConnectionFactory) context.lookup(factoryName);

            // look up the Destination
            dest = (Destination) context.lookup(destName);

            // create the connection
            connection = factory.createConnection();

            // create the session
            session = connection.createSession(
                    false, Session.AUTO_ACKNOWLEDGE);

            // create the receiver
            //je veux recevoir toutes les conventions
            receiver = session.createConsumer(dest);

            // start the connection, to enable message receipt
            connection.start();

            //while(true) {
            for(int i = 0; i < count; ++i){
                Message message = receiver.receive();
                if (message instanceof ObjectMessage) {
                    //on récupère le message
                    ObjectMessage flux = (ObjectMessage) message;
                    //on récupère l'objet dans le message
                    Object preconvention = flux.getObject();
                    //on récupère la pré-conv dans l'objet
                    if (preconvention instanceof PreConvention) {
                        PreConvention convention = (PreConvention) preconvention;
                        
                        if(!conv.containsKey(convention.getId()) && !convTraitees.containsKey(convention.getId()))
                            conv.put(convention.getId(), convention);
                    }

                } else if (message != null) {
                    System.out.println("Pas de préconvention reçue");
                }
            }
        } catch (JMSException exception) {
            exception.printStackTrace();
        } catch (NamingException exception) {
            exception.printStackTrace();
        } finally {
            // close the context
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException exception) {
                    exception.printStackTrace();
                }
            }

            // close the connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }
    
    public void envoyer(PreConvention pc) throws NamingException{
        System.setProperty("java.naming.factory.initial","com.sun.enterprise.naming.SerialInitContextFactory"); 
        System.setProperty("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
        System.setProperty("org.omg.CORBA.ORBInitialPort", "3700"); 
        InitialContext context = new InitialContext();
        
        ConnectionFactory factory = null;
        Connection connection = null;
        String factoryName = "jms/__defaultConnectionFactory";
        String destName = "ConventionEnCours2";
        Destination dest = null;
        int count = 1;
        Session session = null;
        MessageProducer sender = null;
        
        try {
            // create the JNDI initial context.
            context = new InitialContext();

            // look up the ConnectionFactory
            factory = (ConnectionFactory) context.lookup(factoryName);

            // look up the Destination
            dest = (Destination) context.lookup(destName);

            // create the connection
            connection = factory.createConnection();

            // create the session
            session = connection.createSession(
                false, Session.AUTO_ACKNOWLEDGE);

            // create the sender
            sender = session.createProducer(dest);

            // start the connection, to enable message sends
            connection.start();
            //while(true){
                //for (int i = 0; i < count; i++) {
                    ObjectMessage message = session.createObjectMessage();
                    message.setObject(pc);
                    sender.send(message);
                    
                    System.out.println("Sent: " + message.getObject() + "\n est valide " + pc.estValide());
                //}
                //Thread.sleep(1000);
          //  }
            
        } catch (JMSException exception) {
            exception.printStackTrace();
        } catch (NamingException exception) {
            exception.printStackTrace();
        } finally {
            // close the context
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException exception) {
                    exception.printStackTrace();
                }
            }

            // close the connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }
    
    public void estPasInscrit(PreConvention c){
        PreConvention pc = this.conv.get(c.getId());
        pc.setValidite(false);
        this.conv.remove(pc.getId());
        this.convTraitees.putIfAbsent(pc.getId(), pc);
        try {
            this.envoyer(pc);
        } catch (NamingException ex) {
            Logger.getLogger(ServiceScolarite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void estInscrit(PreConvention c){
        this.conv.remove(c.getId());
        this.convTraitees.putIfAbsent(c.getId(), c);
        try {
            this.envoyer(c);
        } catch (NamingException ex) {
            Logger.getLogger(ServiceScolarite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
