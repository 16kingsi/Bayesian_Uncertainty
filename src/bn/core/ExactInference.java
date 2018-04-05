package bn.core;

import bn.core.BayesianNetwork.Node;
import bn.parser.XMLBIFParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 * @author BAC on 3/30/2018.
 */
public class ExactInference {
    String file = "/src/bn/examples/";
    static BayesianNetwork network = null;
    public ExactInference(String file){
        this.file += file;
//        System.out.println(file);
    }

    public static void main(String[] args) {
        try {
             network = new XMLBIFParser().readNetworkFromFile(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        // ---- network is read and stored ----
        ExactInference inference = new ExactInference(args[0]);
        Node query = network.getNodeForVariable(network.getVariableByName(args[1]));
        Assignment evidence = new Assignment();
        evidence.put(network.getVariableByName(args[2]),args[3]);
        evidence.put(network.getVariableByName(args[4]),args[5]);


        System.out.println("You're asking about: "+query.variable+", given: "+evidence);

        Distribution d = inference.enumerationAsk(query,evidence,network);

        System.out.println(d);
        //System.out.println(network.nodes);
    }

        public Distribution enumerationAsk(Node X, Assignment observedEvidence, BayesianNetwork bn) {
            Distribution Q = new Distribution();//X in constructor?

            //for each value xi that X can have
            for (Object v: X.variable.domain){
                Q.put(v,enumerateAll(bn.getVariableListTopologicallySorted(),observedEvidence));
            }

            Q.normalize();
            return Q;
        }


        protected double enumerateAll(List<RandomVariable> vars, Assignment e) {
            double sum = 0;
            if (vars.isEmpty())
                return 1;

            RandomVariable Y = (RandomVariable) vars.toArray()[0];
            System.out.println("Y = "+Y +", and can be "+Y.domain);
            System.out.println("e = "+e);

            //Y is assigned a value in e
            if (e.containsValue(Y)){
                //return P(Y=y | values assigned to Y’s parents in e) × ENUMERATE-ALL(REST(vars), e)
                return network.getProb(Y,e) * enumerateAll(vars.subList(1,vars.size()),e);
            }
            else{

                for (Object o: Y.domain){
                    System.out.println(o);
                    e.set(Y,o);     //e doesn't contain Y?
                    sum +=  network.getProb(((RandomVariable) e.get(o)),e) * enumerateAll(vars.subList(1,vars.size()),e);
                }
                return sum;
            }

        }


}



