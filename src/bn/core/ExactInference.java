package bn.core;

import bn.parser.XMLBIFParser;
import bn.util.ArraySet;
import com.sun.org.glassfish.gmbal.ParameterNames;
import org.xml.sax.SAXException;
import bn.core.BayesianNetwork.Node;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 * @author BAC on 3/30/2018.
 */
public class ExactInference {
    String file = "/Uncertain_Inference/src/bn/examples/";
    static BayesianNetwork network = null;
    public ExactInference(String file){
        this.file += file;
        //System.out.println(file);
    }

    public static void main(String[] args) {
        network = null;

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
        Distribution d = inference.enumerationAsk(network.getNodeForVariable(network.getVariableByName(args[1])),null,network);

        System.out.println(d);
        System.out.println(network.nodes);
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

            if (vars.isEmpty())
                return 1;

            RandomVariable Y = (RandomVariable) vars.toArray()[0];
            Node nodeY = network.getNodeForVariable(Y);

            //Y is assigned a value in e
            if (e.containsValue(Y)){
                //return P(Y=y | values assigned to Y’s parents in e) × ENUMERATE-ALL(REST(vars), e)
                return network.getProb(Y,e) * enumerateAll(vars.subList(1,vars.size()),e);
            }
            else{
                return network.getProb(Y,e) * enumerateAll(vars.subList(1,vars.size()),e);

            }

        }


}



