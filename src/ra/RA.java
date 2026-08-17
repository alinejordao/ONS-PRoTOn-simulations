/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ons.ra;

import ons.DisasterArea;
import ons.Flow;
import java.util.Map;

/**
 * This is the interface that provides some methods for the RA class.
 * These methods basically deal with the simulation interface and with
 the arriving and departing flows.
 
 The Routing Assignment (RA) is a optical networking problem
 that has the goal of maximizing the number of optical connections.
 * 
 * @author onsteam
 */
public interface RA {
    
    public void simulationInterface(ControlPlaneForRA cp);
    
/**
 * Permite que cada algoritmo RA receba parametros opcionais
 * definidos no XML da simulacao.
 *
 * A implementacao padrao nao executa nenhuma acao,
 * preservando compatibilidade com os algoritmos existentes.
 *
 * @param parameters mapa de parametros configurados no elemento <ra>
 */
default void setParameters(Map<String, String> parameters) {
    // Nenhuma acao por padrao.
}    

    public void flowArrival(Flow flow);
    
    public void flowDeparture(long id);
    
    public void disasterArrival(DisasterArea area);   

    public void disasterDeparture();       
    
    public void delayedFlowDeparture(Flow f);
    
    public void delayedFlowArrival(Flow f);

}
