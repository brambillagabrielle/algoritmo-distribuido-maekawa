package br.edu.ifsul.cc.sd2.simulacaomaekawa;

import java.io.Serializable;

public class Mensagem implements Serializable {

    private String operacao;

    public Mensagem(String operacao) {
        this.operacao = operacao;
    }
    
    public String getOperacao() {
        return operacao;
    }

}
