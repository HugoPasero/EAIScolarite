package servicescolarite;

import donnes.preconvention.PreConvention;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Classe permettant de gérer un service scolarité comme décrit ci-dessous.
 * Le service de la scolarité de l’université est en charge de la gestion des 
 * inscriptions des étudiants. Lui seul est habilité pour certifier qu’un 
 * étudiant, caractérisé par son identité et son numéro d’étudiant, est bien 
 * inscrit à l’université pour une formation diplômante particulière (un stage 
 * s’opère dans le cadre d’une formation diplômante). À chaque formation de 
 * niveau Licence ou Master est associé un code et un intitulé (ex. : EIMIBE – 
 * M2 ingénierie de la transformation numérique, EIMIAE – M2 ingénierie des 
 * données et protection). Chaque formation dépend d’un département 
 * d’enseignement disciplinaire (exemple : EIMIBE et EIMIAE dépendent du
 * département « Informatique »).
 * 
 * @author marieroca
 */
public class ServiceScolarite {
    private HashMap<Long, PreConvention> conv;
    private HashMap<Long, PreConvention> convTraitees;
    static MessageConsumer receiver = null;
    static MessageProducer sender = null;
    static Session session = null;

    /**
     * onstructeur du service scolarité qui réceptionne les préconvention envoyées par le serveur web
     * Et qui s'occuppe de vérifier :
     *      - que l'étudiant est inscrit à la faculté dans la formation
     * @param conv map des pré conventions en cours
     * @param convTraitees liste des préconventions traitées par le service enseignement (validées ou refusées)
     */
    public ServiceScolarite(HashMap<Long, PreConvention> conv, HashMap<Long, PreConvention> convTraitees) {
        //Si la liste des préconvention en cours ou traitées sont nulles ont les instancie vides    
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
            //On receptionne les préconvention envoyées par le serveur web
            this.recevoir();
        } catch (NamingException ex) {
            Logger.getLogger(ServiceScolarite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Getter de la map des pré conventions à traiter
     * @return map (id, pré convention) à traiter
     */
    public HashMap<Long, PreConvention> getConv() {
        return conv;
    }

    /**
     * Setter de la map des pré conventions à traiter
     * @param conv map (id, pré convention) à traiter
     */
    public void setConv(HashMap<Long, PreConvention> conv) {
        this.conv = conv;
    }

    /**
     * Getter de la map des pré conventions traitées
     * @return map (id, pré convention) traitées
     */
    public HashMap<Long, PreConvention> getConvTraitees() {
        return convTraitees;
    }

    /**
     * Setter de la map des pré conventions traitées
     * @param conv map (id, pré convention) traitées
     */
    public void setConvTraitees(HashMap<Long, PreConvention> convTraitees) {
        this.convTraitees = convTraitees;
    }
  
    /**
     * Initialisation de la connexion : 
     *      - au topic "Pré-convention" pour réceptionner les préconvention envoyées par le serveur web
     *      - à la queue "Conventions en cours" pour envoyer les conventions traitées (validées ou refusées) au service des stages
     * @throws NamingException
     * @throws JMSException 
     */
    static public void init() throws NamingException, JMSException {
        System.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
        System.setProperty("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
        System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
        InitialContext context = new InitialContext();
        
        ConnectionFactory factory = null;
        Connection connection = null;
        String factoryName = "jms/__defaultConnectionFactory";
        Destination dest = null;

        // look up the ConnectionFactory
        factory = (ConnectionFactory) context.lookup(factoryName);

        //Pour le receiver
        String destName = "PreConvention";
        
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

        //Pour le sender
        destName = "ConventionEnCours2";

        // look up the Destination
        dest = (Destination) context.lookup(destName);

        // create the sender
        sender = session.createProducer(dest);

        // start the connection, to enable message receipt
        connection.start();
    }
    
    /**
     * Méthode permettant de recevoir les messages JMS emits par le serveur web (préconventions à valider)
     * via le sujet de discussion "PreConvention"
     * @return map (id, pré convention)
     * @throws NamingException 
     */
    public void recevoir() throws NamingException {
        try {
            //Dégager le while true à terme
            while(true) {
                Message message = receiver.receiveNoWait();
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
                        else 
                            break;
                    }
                } else if (message != null) {
                    System.out.println("Pas de préconvention reçue");
                }
            }
        } catch (JMSException exception) {
            exception.printStackTrace();
        }
    }
    
    /**
     * Méthode permettant d'envoyer les messages JMS (préconventions validées ou refusées) du service juridique
     * vers le service des stages via la file ConventionEnCours
     * @param pc préconvention à envoyer
     * @throws NamingException
     * @throws InterruptedException 
     */
    public void envoyer(PreConvention pc) throws NamingException{
        try {
            ObjectMessage message = session.createObjectMessage();
            message.setObject(pc);
            sender.send(message);
            
            System.out.println("Sent: " + message.getObject() + "\n est valide " + pc.estValide());
        } catch (JMSException ex) {
            Logger.getLogger(ServiceScolarite.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    /**
     * Méthode permettant de mettre la préconvention non valide et de l'envoyer au bureau de stages si l'étudiant n'est pas inscrit
     * @param c préconvention où l'étudiant n'est pas inscrit
     */
    public void estPasInscrit(PreConvention c){
        PreConvention pc = this.conv.get(c.getId());
        //la préconvention n'est pas valide
        pc.setValidite(false);
        //On bouge la préconvention de la liste des conventions à traiter vers les conventions traitées
        this.conv.remove(pc.getId());
        this.convTraitees.putIfAbsent(pc.getId(), pc);
        try {
            //on envoie la préconvention au bureau des stages
            this.envoyer(pc);
        } catch (NamingException ex) {
            Logger.getLogger(ServiceScolarite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Méthode permettant de mettre la préconvention validée et de l'envoyer au bureau de stages si l'étudiant est inscrit
     * @param c préconvention où l'étudiant est inscrit
     */
    public void estInscrit(PreConvention c){
        //On bouge la préconvention de la liste des conventions à traiter vers les conventions traitées
        this.conv.remove(c.getId());
        this.convTraitees.putIfAbsent(c.getId(), c);
        try {
            //on envoie la préconvention au bureau des stages
            this.envoyer(c);
        } catch (NamingException ex) {
            Logger.getLogger(ServiceScolarite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
