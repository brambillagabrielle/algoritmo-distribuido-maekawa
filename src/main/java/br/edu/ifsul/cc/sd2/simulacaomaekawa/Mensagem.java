package br.edu.ifsul.cc.sd2.simulacaomaekawa;

import java.io.Serializable;

/**
 * A classe Mensagem é usada para representar mensagens que contêm uma operação associada
 * Ela implementa a interface Serializable, permitindo que objetos desta classe sejam convertidos em bytes
 * para fins de transmissão ou armazenamento
 *
 * @author Estéfani Ferlin e Gabrielle Brambilla
 */
public class Mensagem implements Serializable {

    private String operacao;

    /**
     * Construtor da classe Mensagem
     *
     * @param operacao A operação que será associada à mensagem
     */
    public Mensagem(String operacao) {
        this.operacao = operacao;
    }
    
    /**
     * Método que obtém a operação associada à mensagem
     *
     * @return A operação associada à mensage
     */
    public String getOperacao() {
        return operacao;
    }

}
